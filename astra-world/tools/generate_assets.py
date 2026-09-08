#!/usr/bin/env python3
"""Reproducible original branding derivatives and layered stereo sound design.
The supplied icon-art.png is an original imagegen result, not a runtime dependency.
No downloaded sample packs, services, or copyrighted music are used.
"""
from pathlib import Path
import math, subprocess, wave, shutil
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
BRAND=ROOT/'assets/branding'
AUDIO=ROOT/'assets/audio'
SR=22050
rng=np.random.default_rng(20260908)

def branding():
    icon=Image.open(BRAND/'icon-art.png').convert('RGB')
    icon.resize((512,512),Image.Resampling.LANCZOS).save(BRAND/'icon.png')
    for source,dest,size in [('crest.svg','foreground.png',432),('background.svg','background.png',432),('crest.svg','splash.png',256)]:
        subprocess.run(['inkscape',str(BRAND/source),'--export-type=png',f'--export-width={size}',f'--export-height={size}','--export-filename='+str(BRAND/dest)],check=True)
    fg=Image.open(BRAND/'foreground.png').convert('RGBA'); mono=Image.new('RGBA',fg.size,'white'); mono.putalpha(fg.getchannel('A')); mono.save(BRAND/'monochrome.png')
    for dst,src in [('body.ttf','DejaVuSans.ttf'),('display.ttf','DejaVuSerif.ttf')]:
        if not (BRAND/dst).exists(): shutil.copy('/usr/share/fonts/truetype/dejavu/'+src,BRAND/dst)

def note(midi): return 440*2**((midi-69)/12)
def env(n,attack=.04,release=.3):
    e=np.ones(n); a=min(n,int(SR*attack)); r=min(n,int(SR*release))
    if a:e[:a]=np.sin(np.linspace(0,math.pi/2,a))**2
    if r:e[-r:]*=np.linspace(1,0,r)**1.5
    return e

def instrument(midi,duration,kind):
    t=np.arange(int(SR*duration))/SR; f=note(midi); n=len(t)
    if kind=='pad':
        phase=2*np.pi*f*t+np.sin(t*4.8)*.018
        y=sum(np.sin(phase*k+(.006*k*t))*a for k,a in [(1,.6),(2,.14),(3,.085),(4,.035)])
        y+=np.sin(2*np.pi*f*1.002*t)*.18
        return y*env(n,.8,.9)*.35
    if kind=='flute':
        phase=2*np.pi*f*t+np.sin(t*31)*.025
        breath=rng.normal(0,.035,n)
        breath=np.convolve(breath,np.ones(8)/8,mode='same')
        y=(np.sin(phase)*.65+np.sin(phase*2)*.12+np.sin(phase*3)*.04+breath)
        return y*env(n,.12,.28)*(.9+.1*np.sin(t*3))*.35
    if kind=='harp':
        y=sum(np.sin(2*np.pi*f*k*t+rng.uniform(-.1,.1))*np.exp(-t*(1.5+k*.55))/k**1.8 for k in range(1,8))
        return y*env(n,.008,.15)*.40
    if kind=='bell':
        y=sum(np.sin(2*np.pi*f*k*t)*np.exp(-t*(1+k*.12))*a for k,a in [(1,.65),(2.01,.23),(2.76,.13),(4.07,.07)])
        return y*env(n,.006,.25)*.4
    return np.sin(2*np.pi*f*t)*env(n,.05,.2)*.25

def write(path,y,normalize=True):
    if normalize: y=y/(max(1,np.max(np.abs(y)))*1.12)
    else: y=np.clip(y,-.95,.95)
    with wave.open(str(path),'wb') as w:
        w.setnchannels(2 if y.ndim==2 else 1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((y*32767).astype('<i2').tobytes())

def track(name,combat=False,home=False):
    bpm=96 if combat else 72; beat=60/bpm; bars=16; duration=bars*4*beat; count=int(duration*SR)
    mix=np.zeros((count,2),np.float64)
    def add(midi,start,dur,kind,gain=.5,pan=0):
        y=instrument(midi,dur,kind)*gain; start=int(start*SR)
        ids=(np.arange(len(y))+start)%count
        mix[ids,0]+=y*np.sqrt((1-pan)/2); mix[ids,1]+=y*np.sqrt((1+pan)/2)
    chords=[[50,57,62,65],[46,53,58,62],[53,60,65,69],[48,55,60,64]]
    melody=[74,77,81,79,77,74,72,69,70,74,77,76,74,72,69,67]
    for bar in range(bars):
        chord=chords[bar%4]; start=bar*4*beat
        for i,n in enumerate(chord): add(n,start,4.5*beat,'pad',.20 if combat else .38,(-.65+i*.42))
        for j in range(8):
            n=chord[j%4]+(12 if j%3==0 else 0)
            add(n,start+j*beat/2,1.8,'harp',.52 if home else .32,math.sin(j)*.5)
        if bar%2==0:
            for j in range(4): add(melody[(bar//2*4+j)%16],start+j*beat*1.5,1.35*beat,'flute',.5,0.12)
        if bar%4==0:add(chord[2]+24,start+.15,3,'bell',.22,-.5)
        if combat:
            for j in range(4):
                t=np.arange(int(SR*.36))/SR; drum=np.sin(2*np.pi*(65*t-30*t*t))*np.exp(-t*12)*.22
                begin=int((start+j*beat)*SR); end=min(count,begin+len(drum)); mix[begin:end,:]+=drum[:end-begin,None]
    # Circular room tails preserve seamless looping. Decorrelated delays create depth.
    dry=mix.copy()
    for delay,gain in [(0.173,.18),(.271,.13),(.419,.10),(.683,.08),(1.13,.045)]:
        mix+=np.roll(dry,int(delay*SR),axis=0)[:,::-1]*gain
    mix=np.tanh(mix*1.15)*.77
    wav=AUDIO/(name+'.wav'); write(wav,mix)
    subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','4',str(AUDIO/(name+'.ogg'))],check=True); wav.unlink()

def effects():
    for name in ['step','swing','hit','skill','dash','hurt','heal','warning','slam','gather','ui','beacon']:
        dur={'step':.14,'swing':.28,'hit':.3,'skill':1.1,'dash':.35,'hurt':.35,'heal':1.2,'warning':.55,'slam':.75,'gather':.65,'ui':.09,'beacon':3.0}[name]
        t=np.arange(int(SR*dur))/SR; noise=rng.normal(0,1,len(t)); soft=np.convolve(noise,np.ones(12)/12,'same')
        if name=='step':y=soft*np.exp(-t*32)*.6+np.sin(2*np.pi*90*t)*np.exp(-t*50)*.13
        elif name in ['swing','dash']:y=soft*np.sin(np.pi*t/dur)**1.3*.8
        elif name in ['hit','hurt','slam']:y=soft*np.exp(-t*14)*.45+np.sin(2*np.pi*(95*t-22*t*t))*np.exp(-t*9)*.5
        elif name=='warning':y=(np.sin(2*np.pi*260*t)+np.sin(2*np.pi*263*t))*.15*env(len(t),.03,.2)
        elif name=='ui':y=np.sin(2*np.pi*1100*t)*np.exp(-t*55)*.14
        else:
            y=np.zeros(len(t)); notes={'skill':[74,81,86],'heal':[69,74,77],'gather':[81,86],'beacon':[62,69,74,77,81]}[name]
            for i,n in enumerate(notes):
                start=int(i*.08*SR); v=instrument(n,max(.1,dur-i*.08),'bell')*.5; y[start:start+min(len(v),len(y)-start)]+=v[:len(y)-start]
            y+=soft*np.sin(np.pi*t/dur)**2*.06
        write(AUDIO/(name+'.wav'),y*env(len(y),.003,.025))
    dur=32; t=np.arange(SR*dur)/SR; noise=rng.normal(0,1,len(t)); wind=np.convolve(noise,np.ones(140)/140,'same')
    y=wind*(.38+.12*np.sin(t*.5))
    for at in [3,7.8,13.4,21.3,26]:
        tt=np.arange(int(SR*.5))/SR; chirp=np.sin(2*np.pi*(2100*tt+900*tt**2))*np.sin(np.pi*tt/.5)**2*.035
        start=int(at*SR); y[start:start+len(chirp)]+=chirp
    stereo=np.column_stack([y,np.roll(y,240)])
    wav=AUDIO/'ambience.wav'; write(wav,stereo)
    subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','3',str(AUDIO/'ambience.ogg')],check=True); wav.unlink()
    for p in AUDIO.glob('*.ogg'):
        Path(str(p)+'.import').write_text('[remap]\nimporter="oggvorbisstr"\ntype="AudioStreamOggVorbis"\n\n[params]\nloop=true\nloop_offset=0\n')

if __name__=='__main__':
    AUDIO.mkdir(parents=True,exist_ok=True); branding(); effects()
    track('aeralis'); track('home',home=True); track('combat',combat=True)
    # Audio import loop parameters apply to every produced music/ambience file.
    for p in AUDIO.glob('*.ogg'):
        Path(str(p)+'.import').write_text('[remap]\nimporter="oggvorbisstr"\ntype="AudioStreamOggVorbis"\n\n[params]\nloop=true\nloop_offset=0\n')
    print('Astra World: branding, three original scores, ambience, and 12 SFX generated.')
