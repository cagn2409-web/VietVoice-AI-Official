"""Keep sherpa's exact versioned ORT ABI separate from Java ORT.
Usage: python isolate_sherpa_runtime.py original.aar output.aar /path/to/patchelf
Requires patchelf 0.18+; preserves models, classes and other archive entries.
"""
import sys,zipfile,tempfile,subprocess
from pathlib import Path
source,target,patch=sys.argv[1:]
old='libonnxruntime.so';new='libonnxruntime_sherpa.so'
with tempfile.TemporaryDirectory() as tmp,zipfile.ZipFile(source) as src,zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as dst:
 for i in src.infolist():
  data=src.read(i);name=i.filename
  if name.endswith('.so'):
   p=Path(tmp)/Path(name).name;p.write_bytes(data)
   needed=subprocess.check_output([patch,'--print-needed',str(p)],text=True).splitlines()
   if old in needed:subprocess.run([patch,'--page-size','16384','--replace-needed',old,new,str(p)],check=True)
   if Path(name).name==old:
    subprocess.run([patch,'--page-size','16384','--set-soname',new,str(p)],check=True)
    name=name[:-len(old)]+new
   data=p.read_bytes()
  dst.writestr(name,data)
