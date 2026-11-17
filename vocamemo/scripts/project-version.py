import pathlib,re
config=(pathlib.Path(__file__).resolve().parents[1]/'app/build.gradle.kts').read_text()
print(re.search(r'versionName = "([0-9.]+)"',config).group(1))
