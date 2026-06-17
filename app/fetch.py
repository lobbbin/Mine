import urllib.request
f = urllib.request.urlopen("https://github.com/Free-AI-Things/g4f-working")
print(f.read().decode('utf-8'))
