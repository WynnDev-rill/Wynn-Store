"""Native Vulkan playtest. X11, Godot and xdotool share one network namespace.
Driver JSON: {id,click:[x,y]} / {id,key:'Escape'} / {id,down:'j'} / {id,up:'j'} /
{id,capture:'/absolute/path.png'} / {id,quit:true}.
"""
import os,sys,time,json,socket,subprocess,pathlib
project=pathlib.Path(__file__).resolve().parents[1];tc=pathlib.Path(sys.argv[1]).resolve();qa=pathlib.Path(sys.argv[2]).resolve();qa.mkdir(parents=True,exist_ok=True)
env=os.environ.copy();env.update({'DISPLAY':'127.0.0.1:95','XDG_DATA_HOME':str(qa/'profile'),'LD_LIBRARY_PATH':str(tc/'sysroot/usr/lib/x86_64-linux-gnu')+':'+str(tc/'sysroot/usr/lib'),'VK_DRIVER_FILES':str(tc/'sysroot/usr/share/vulkan/icd.d/lvp_icd.json')})
xlog=open(qa/'xvfb.log','w');x=subprocess.Popen([str(tc/'sysroot/usr/bin/Xvfb'),':95','-screen','0','1280x720x24','-listen','tcp','-nolisten','unix','-nolisten','local','-ac'],env=env,stdout=xlog,stderr=xlog)
try:
 for i in range(80):
  try:s=socket.create_connection(('127.0.0.1',6095),.2);s.close();break
  except OSError:time.sleep(.1)
 log=open(qa/'runtime.log','w');game=subprocess.Popen([str(tc/'godot'),'--path',str(project),'--audio-driver','Dummy','--position','0,0','--resolution','1280x720','--','--qa','--qa-dir='+str(qa)],env=env,stdout=log,stderr=log);last=-1;print('SESSION_STARTED',game.pid,flush=True)
 while game.poll() is None:
  f=qa/'driver-command.json'
  try:c=json.loads(f.read_text()) if f.exists() else None
  except (OSError,json.JSONDecodeError):c=None
  if c and c.get('id',-1)!=last:
   last=c['id'];cmd=[str(tc/'sysroot/usr/bin/xdotool')]
   if 'click' in c:subprocess.run(cmd+['mousemove','--sync',str(c['click'][0]),str(c['click'][1]),'click','1'],env=env,check=True)
   for key in ['key','down','up']:
    if key in c:subprocess.run(cmd+[{'key':'key','down':'keydown','up':'keyup'}[key],c[key]],env=env,check=True)
   if 'capture' in c:
    p=qa/'command.tmp';p.write_text(json.dumps({'id':last,'capture':c['capture']}));p.replace(qa/'command.json')
   (qa/'driver-ack.json').write_text(json.dumps({'id':last}))
   if c.get('quit'):game.terminate();break
  time.sleep(.03)
 print('SESSION_EXIT',game.wait(timeout=10),flush=True)
finally:x.terminate()
