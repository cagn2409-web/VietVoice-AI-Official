"""Regression check for the versioned OrtGetApiBase Android linker failure."""
import sys,zipfile,tempfile,subprocess,re
from pathlib import Path
with zipfile.ZipFile(sys.argv[1]) as z,tempfile.TemporaryDirectory() as tmp:
 for abi in ['arm64-v8a','armeabi-v7a']:
  root=Path(tmp)/abi;root.mkdir()
  for name in z.namelist():
   if name.startswith('lib/'+abi+'/') and name.endswith('.so'):(root/Path(name).name).write_bytes(z.read(name))
  for p in root.glob('*.so'):
   syms=subprocess.check_output(['readelf','-Ws',str(p)],text=True)
   required=re.findall(r'UND (Ort\w+)@(VERS_[\d.]+)',syms)
   if not required:continue
   versions=subprocess.check_output(['readelf','-V',str(p)],text=True)
   needed=subprocess.check_output(['readelf','-d',str(p)],text=True)
   for symbol,version in required:
    providers=[]
    for entry in re.split(r'(?=File: )',versions)[1:]:
     match=re.match(r'File: (\S+)',entry)
     if match and 'Name: '+version+' ' in entry:providers.append(match.group(1))
    assert len(providers)==1,(p.name,symbol,version,providers)
    lib=providers[0];assert '['+lib+']' in needed,(p.name,lib,'not DT_NEEDED')
    provider=root/lib;assert provider.exists(),(p.name,'missing',lib)
    exports=subprocess.check_output(['readelf','-Ws',str(provider)],text=True)
    assert re.search(r'\b'+symbol+r'@@?'+re.escape(version)+r'\b',exports),(abi,p.name,symbol,version,lib,'ABI mismatch')
   print(abi,p.name,'versioned ORT symbols matched')
