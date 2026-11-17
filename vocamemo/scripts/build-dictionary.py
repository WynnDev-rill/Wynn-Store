#!/usr/bin/env python3
"""Deterministically adapt official WikDict SQLite, with optional Tatoeba pairs.
Source and derivative are CC BY-SA 4.0; examples retain separate CC BY 2.0 credits.
No translation is authored by this script.
"""
import argparse, sqlite3, pathlib, hashlib, json, re, zipfile
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
CREATE TABLE example(id INTEGER PRIMARY KEY,english TEXT NOT NULL,indonesian TEXT NOT NULL,credit TEXT NOT NULL);
CREATE VIRTUAL TABLE example_fts USING fts4(english,content='example',tokenize=unicode61);
CREATE TABLE metadata(key TEXT PRIMARY KEY,value TEXT NOT NULL);
''')
rows=s.execute('select lexentry,written_rep,sense,trans_list from translation where is_good=1 order by lower(written_rep),lexentry,coalesce(sense,""),trans_list').fetchall()
cache={}
for i,(lex,word,ctx,tr) in enumerate(rows,1):
 if lex not in cache:
  meta=e.execute('select part_of_speech,pronun_list from entry where lexentry=?',(lex,)).fetchone()
  cache[lex]=meta or ('','')
 pos,ipa=cache[lex]
 c.execute('insert into sense values(?,?,?,?,?,?,?,?)',(i,word,word.casefold(),pos or lex.split('__')[-2],ctx or '',tr,ipa or '',lex))
nexamples=0
z=a.sources/'ind-eng.zip'
if z.exists():
 with zipfile.ZipFile(z) as archive:
  for line in archive.read('ind.txt').decode('utf-8-sig').splitlines():
   cols=line.split('\t')
   if len(cols)>=3 and cols[0] and cols[1]:
    c.execute('insert into example(english,indonesian,credit) values(?,?,?)',cols[:3]); nexamples+=1
 c.execute("insert into example_fts(example_fts) values('rebuild')")
manifest={'version':'2_2026-06','source':'WikDict / Wiktionary contributors via DBnary','license':'CC BY-SA 4.0','senses':len(rows),'words':c.execute('select count(distinct search) from sense').fetchone()[0],'examples':nexamples,'excluded_low_quality':s.execute('select count(*) from translation where is_good=0').fetchone()[0],'inputs':{f.name:hashlib.sha256(f.read_bytes()).hexdigest() for f in [a.sources/'en-id.sqlite3',a.sources/'en.sqlite3']+([z] if z.exists() else [])},'changes':'is_good=1 filter; indexed search; IPA/POS joined by exact lexentry; Tatoeba independent examples, not sense assertions'}
c.executemany('insert into metadata values(?,?)',[(k,str(v)) for k,v in manifest.items()]);c.execute('pragma user_version=1');c.commit();c.execute('vacuum');c.close();tmp.replace(a.output)
manifest['sha256']=hashlib.sha256(a.output.read_bytes()).hexdigest()
a.output.with_suffix('.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(json.dumps(manifest,indent=2))
