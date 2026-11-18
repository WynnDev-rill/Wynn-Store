#!/usr/bin/env python3
"""Deterministically adapt official WikDict SQLite, with optional Tatoeba pairs.
Source and derivative are CC BY-SA 4.0; examples retain separate CC BY 2.0 credits.
No translation is authored by this script.
"""
import argparse, sqlite3, pathlib, hashlib, json, zipfile, unicodedata
p=argparse.ArgumentParser(); p.add_argument('--sources',type=pathlib.Path,required=True); p.add_argument('--output',type=pathlib.Path,default=pathlib.Path(__file__).resolve().parents[1]/'app/src/main/assets/dictionary.db'); a=p.parse_args()
a.output.parent.mkdir(parents=True,exist_ok=True)
tmp=a.output.with_suffix('.tmp'); tmp.unlink(missing_ok=True)
s=sqlite3.connect(a.sources/'en-id.sqlite3'); e=sqlite3.connect(a.sources/'en.sqlite3')
assert s.execute('pragma integrity_check').fetchone()[0]=='ok'
assert e.execute('pragma integrity_check').fetchone()[0]=='ok'
c=sqlite3.connect(tmp)
c.executescript('''
CREATE TABLE sense(id INTEGER PRIMARY KEY,word TEXT NOT NULL,search TEXT NOT NULL,pos TEXT NOT NULL,context TEXT NOT NULL,translations TEXT NOT NULL,ipa TEXT NOT NULL,lexentry TEXT NOT NULL);
CREATE INDEX sense_search ON sense(search);
CREATE TABLE headword(search TEXT PRIMARY KEY, word TEXT NOT NULL, summary TEXT NOT NULL, senses INTEGER NOT NULL, sort_key TEXT NOT NULL, relevance REAL NOT NULL);
CREATE INDEX headword_sort ON headword(sort_key);
CREATE TABLE lookup(language TEXT NOT NULL, term TEXT NOT NULL, headword TEXT NOT NULL, gloss TEXT NOT NULL, inflected INTEGER NOT NULL, length INTEGER NOT NULL, relevance REAL NOT NULL, PRIMARY KEY(language,term,headword,gloss,inflected)) WITHOUT ROWID;
CREATE INDEX lookup_length ON lookup(language,inflected,length,term);
CREATE TABLE example(id INTEGER PRIMARY KEY,english TEXT NOT NULL,indonesian TEXT NOT NULL,credit TEXT NOT NULL);
CREATE VIRTUAL TABLE example_fts USING fts4(english,content='example',tokenize=unicode61);
CREATE TABLE metadata(key TEXT PRIMARY KEY,value TEXT NOT NULL);
''')
def normalize(text):
    return " ".join(unicodedata.normalize("NFC", text).replace("’", "'").lower().split())

rows=s.execute('select lexentry,written_rep,sense,trans_list,coalesce(score,0) from translation where is_good=1 order by lower(written_rep),lexentry,coalesce(sense,""),trans_list').fetchall()
lexical_entries={}
headwords={}
lookup={}
def add_lookup(language, term, key, gloss, inflected, relevance):
    term=normalize(term)
    if term and len(term)<=200:
        identity=(language,term,key,gloss,inflected,len(term))
        lookup[identity]=max(lookup.get(identity,0), relevance)

for i,(lex,word,ctx,tr,score) in enumerate(rows,1):
    key=normalize(word)
    meta=lexical_entries.get(lex)
    if meta is None:
        meta=e.execute('select part_of_speech,pronun_list from entry where lexentry=?',(lex,)).fetchone() or ('','')
        lexical_entries[lex]=meta
    pos,ipa=meta
    c.execute('insert into sense values(?,?,?,?,?,?,?,?)',(i,word,key,pos or lex.split('__')[-2],ctx or '',tr,ipa or '',lex))
    headwords.setdefault(key,[]).append((score,word,tr,ctx or ''))
    for gloss in tr.split(' | '):
        add_lookup('id',gloss,key,gloss,0,score)
    for form, in e.execute('select other_written from form where lexentry=? and other_written is not null',(lex,)):
        if normalize(form)!=key:
            add_lookup('en',form,key,'',1,score)

for key,items in sorted(headwords.items()):
    ranked=sorted(items,key=lambda row:(-row[0],len(row[3]),row[2],row[1]))
    score,word,_,_=ranked[0]
    glosses=list(dict.fromkeys(g for _,_,tr,_ in ranked for g in tr.split(' | ')))[:3]
    alphabet=unicodedata.normalize('NFKD',key.lstrip(" -'0123456789")).encode('ascii','ignore').decode()
    c.execute('insert into headword values(?,?,?,?,?,?)',(key,word,' · '.join(glosses),len(items),(alphabet if alphabet and 'a'<=alphabet[0]<='z' else '{'+alphabet)+'\x01'+key,score))
    add_lookup('en',key,key,'',0,score)

# WordNet contributes lexical aliases only, never meanings.
with zipfile.ZipFile(a.sources/'wordnet.zip') as wordnet:
    for pos in ('noun','verb','adj','adv'):
        for line in wordnet.read(f'wordnet/{pos}.exc').decode().splitlines():
            form,*lemmas=[word.replace('_',' ') for word in line.split()]
            for lemma in lemmas:
                if normalize(lemma) in headwords and form!=lemma:
                    add_lookup('en',form,normalize(lemma),'',1,max(row[0] for row in headwords[normalize(lemma)]))
c.executemany('insert into lookup values(?,?,?,?,?,?,?)',[(*entry,score) for entry,score in sorted(lookup.items())])
nexamples=0
z=a.sources/'ind-eng.zip'
if z.exists():
 with zipfile.ZipFile(z) as archive:
  for line in archive.read('ind.txt').decode('utf-8-sig').splitlines():
   cols=line.split('\t')
   if len(cols)>=3 and cols[0] and cols[1]:
    c.execute('insert into example(english,indonesian,credit) values(?,?,?)',cols[:3]); nexamples+=1
 c.execute("insert into example_fts(example_fts) values('rebuild')")
manifest={'version':'2_2026-06','source':'WikDict / Wiktionary contributors via DBnary','license':'CC BY-SA 4.0','senses':len(rows),'words':len(headwords),'examples':nexamples,'lookup_rows':len(lookup),'excluded_low_quality':s.execute('select count(*) from translation where is_good=0').fetchone()[0],'inputs':{f.name:hashlib.sha256(f.read_bytes()).hexdigest() for f in [a.sources/'en-id.sqlite3',a.sources/'en.sqlite3',a.sources/'wordnet.zip']+([z] if z.exists() else [])},'changes':'is_good=1 filter; indexed search; IPA/POS joined by exact lexentry; Tatoeba independent examples, not sense assertions; scored browse summaries, reverse index and attested lexical forms'}
c.executemany('insert into metadata values(?,?)',[(k,str(v)) for k,v in manifest.items()]);c.execute('pragma user_version=2');c.commit();c.execute('vacuum');c.close();tmp.replace(a.output)
manifest['sha256']=hashlib.sha256(a.output.read_bytes()).hexdigest()
a.output.with_suffix('.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(json.dumps(manifest,indent=2))
