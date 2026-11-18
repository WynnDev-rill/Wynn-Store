#!/usr/bin/env python3
import sqlite3,json,pathlib,hashlib,time
p=pathlib.Path(__file__).resolve().parents[1]/'app/src/main/assets/dictionary.db'
m=json.loads(p.with_suffix('.json').read_text());c=sqlite3.connect(p)
assert hashlib.sha256(p.read_bytes()).hexdigest()==m['sha256']
assert c.execute('pragma integrity_check').fetchone()[0]=='ok'
assert c.execute('select count(*) from sense').fetchone()[0]==20230
assert c.execute('select count(distinct search) from sense').fetchone()[0]==15725
assert c.execute("select count(*) from sense where word='' or translations='' or pos=''").fetchone()[0]==0
for word,meaning,pos in [('apple','apel','noun'),('book','buku','noun'),('book','pesan','verb'),('light','ringan','adjective'),('light','cahaya','noun'),('hello','hai','interjection'),('run','berlari','verb')]:
 rows=c.execute('select translations,pos from sense where search=?',(word,)).fetchall()
 assert any(meaning in t.split(' | ') and p.lower()==pos for t,p in rows),(word,meaning,rows)
assert c.execute("select count(*) from example where credit not like '%tatoeba.org%' or indonesian='' or english=''").fetchone()[0]==0
assert c.execute('select count(*) from example').fetchone()[0]==15589
assert c.execute("select count(*) from example_fts where example_fts match '\"book\"'").fetchone()[0]>0
plan=c.execute("explain query plan select id from sense where search='apple'").fetchall()
assert any('INDEX' in str(x) for x in plan),plan
start=time.perf_counter()
for _ in range(100):c.execute("select * from sense where search like 'app%' limit 80").fetchall()
print(json.dumps({'status':'PASS','words':15725,'senses':20230,'examples':15589,'prefix_mean_ms':round((time.perf_counter()-start)*10,3),'exact_query_plan':plan},indent=2))
headwords = c.execute("select count(*) from headword").fetchone()[0]
cursor = ""
scanned = 0
while True:
    page = c.execute("select sort_key from headword where sort_key>? order by sort_key limit 64", (cursor,)).fetchall()
    if not page:
        break
    for (key,) in page:
        assert key > cursor
        cursor = key
        scanned += 1
assert scanned == headwords == m["words"]
assert c.execute("select count(*) from lookup").fetchone()[0] == m["lookup_rows"]
assert c.execute("select count(*) from lookup l left join headword h on h.search=l.headword where h.search is null").fetchone()[0] == 0
for lang, term, headword, gloss, form in [
    ("id", "buku", "book", "buku", 0),
    ("en", "books", "book", "", 1),
    ("en", "running", "run", "", 1),
    ("en", "went", "go", "", 1),
]:
    assert c.execute("select 1 from lookup where language=? and term=? and headword=? and gloss=? and inflected=?", (lang, term, headword, gloss, form)).fetchone(), term
print(f"Browse cursor audit passed: {scanned:,} headwords and {m['lookup_rows']:,} lookup rows.")
