"""AstraSlash original score / effects. Deterministic, no samples or external music.
Requires numpy and ffmpeg. Run from any directory. Musical composition CC0.
"""
from pathlib import Path
import numpy as np
import subprocess, wave

ROOT = Path(__file__).resolve().parents[1] / 'assets/audio'
ROOT.mkdir(parents=True, exist_ok=True)
SR = 32000
rng = np.random.default_rng(73421)

def hz(note): return 440.0 * 2 ** ((note - 69) / 12)
def tone(note, length, kind='pluck', amp=.15):
    t = np.arange(int(SR * length)) / SR
    f = hz(note)
    if kind == 'pad':
        a = (np.sin(2*np.pi*f*t) + .35*np.sin(2*np.pi*(f*1.003)*t) + .18*np.sin(4*np.pi*f*t))
        env = np.minimum(t/.4, 1) * np.minimum((length-t)/.7, 1)
    elif kind == 'bell':
        a = np.sin(2*np.pi*f*t + 1.8*np.sin(2*np.pi*f*2*t)*np.exp(-t*4))
        env = np.exp(-t*3) * np.minimum(t/.006, 1)
    elif kind == 'bass':
        a = np.sin(2*np.pi*f*t)+.2*np.sin(4*np.pi*f*t)
        env = np.exp(-t*3) * np.minimum(t/.009, 1)
    else:
        a = np.sin(2*np.pi*f*t)+.25*np.sin(4*np.pi*f*t)+.1*np.sin(6*np.pi*f*t)
        env = np.exp(-t*5) * np.minimum(t/.004, 1)
    return a * env * amp

def save(name, data, music=False):
    data = np.tanh(data * .85)
    if data.ndim == 1: data = np.stack([data, np.roll(data, int(.002*SR))], axis=1)
    peak = np.max(np.abs(data))
    if peak > .94: data *= .94 / peak
    wav = ROOT / (name + '.wav')
    with wave.open(str(wav), 'wb') as w:
        w.setparams((2, 2, SR, 0, 'NONE', 'not compressed'))
        w.writeframes((data * 32767).astype('<i2').tobytes())
    if music:
        subprocess.run(['ffmpeg','-y','-loglevel','error','-i',str(wav),'-c:a','libvorbis','-q:a','4',str(ROOT/(name+'.ogg'))],check=True)
        wav.unlink()
    print(name, flush=True)

def music(name, bpm, transpose=0, intensity=1):
    beat = 60 / bpm
    length = 64 * beat
    out = np.zeros((int(SR*length),2))
    def put(signal, time, pan=0):
        pos = int(time*SR)
        stereo = np.stack([signal*(.7-.25*pan),signal*(.7+.25*pan)],axis=1)
        for start in range(0, len(signal), len(out)):
            block=stereo[start:start+len(out)]
            ids=(np.arange(len(block))+pos+start)%len(out)
            out[ids]+=block
    # D minor / Bb major / F major / C suspended. Four repeated 4-bar phrases.
    chords=[[50,57,65],[46,53,62],[53,60,69],[48,55,62]]
    motif=[0,2,4,2,1,2,5,4,2,1,0,2,4,5,4,2]
    scale=[74,77,79,81,84,86]
    for bar in range(16):
        chord=[n+transpose for n in chords[(bar//1)%4]]
        for n in chord: put(tone(n,5*beat,'pad',.075),bar*4*beat, -.2)
        for b in range(4):
            put(tone(chord[0]-12,.75*beat,'bass',.23 if intensity else .10),(bar*4+b)*beat)
        for s in range(8):
            n=chord[[0,1,2,1,0,2,1,2][s]]+12
            put(tone(n,beat*.85,'bell',.075),(bar*4+s/2)*beat,(-1)**s*.65)
        if bar%4>=2 or not intensity:
            for s in range(4):
                n=scale[motif[(bar*4+s)%len(motif)]]+transpose
                put(tone(n,beat*1.3,'pluck',.12),(bar*4+s)*beat,.12)
        if intensity:
            for b in range(8):
                tm=(bar*4+b/2)*beat
                t=np.arange(int(SR*.12))/SR
                hat=rng.normal(0,1,len(t))*np.exp(-t*65)*.045
                put(hat,tm,(-1)**b*.5)
            for b in (0,2):
                t=np.arange(int(SR*.32))/SR
                kick=np.sin(2*np.pi*(46*t+5*(1-np.exp(-t*22))))*np.exp(-t*15)*.55
                put(kick,(bar*4+b)*beat)
            for b in (1,3):
                t=np.arange(int(SR*.22))/SR
                snare=(rng.normal(0,1,len(t))*.22+np.sin(2*np.pi*175*t)*.2)*np.exp(-t*20)
                put(snare,(bar*4+b)*beat,.08)
    # Circular stereo musical delay keeps the loop boundary continuous.
    for delay,gain in ((.75*beat,.16),(1.5*beat,.08)):
        out+=np.roll(out[:,::-1],int(delay*SR),axis=0)*gain
    save(name,out*.8,True)

music('orbit_home',100,0,0)
music('arunika',140,0,1)
music('glass_garden',132,5,1)
music('eclipse_core',150,-2,1)
music('boss',160,0,1)

for i in range(3):
    t=np.arange(int(SR*(.24+.04*i)))/SR
    noise=rng.normal(0,1,len(t));noise=np.convolve(noise,[.2,.6,.2],'same')
    sweep=np.sin(2*np.pi*((1200-180*i)*t-1600*t*t))
    env=np.sin(np.pi*np.minimum(t/(.24+.04*i),1))**1.3*np.exp(-t*7)
    save('slash'+str(i+1),(noise*.45+sweep*.09)*env)
for name,low,brightness in [('hit',76,.22),('heavy',44,.35),('hurt',100,.18),('break',36,.44)]:
    t=np.arange(int(SR*.55))/SR
    impact=np.sin(2*np.pi*(low*t+2*(1-np.exp(-t*30))))*np.exp(-t*14)
    noise=rng.normal(0,1,len(t))*np.exp(-t*35)*brightness
    metal=np.sin(2*np.pi*1637*t)*np.exp(-t*22)*.12
    save(name,impact*.65+noise+metal)
t=np.arange(int(SR*.38))/SR
save('dash',rng.normal(0,1,len(t))*np.sin(np.pi*t/.38)**2*np.exp(-t*6)*.3)
save('shot',tone(89,.25,'bell',.32)+tone(77,.25,'pluck',.18))
save('jump',np.sin(2*np.pi*(300*t+600*t*t))*np.exp(-t*15)*.12)
save('ui',tone(86,.12,'bell',.18))
save('parry',tone(93,.65,'bell',.3)+tone(100,.65,'bell',.13))
save('loot',tone(81,.6,'bell',.2)+np.pad(tone(88,.48,'bell',.16),(int(SR*.12),0)))
t=np.arange(int(SR*1.8))/SR
save('ultimate',np.sin(2*np.pi*(48*t+9*(1-np.exp(-t*5))))*np.exp(-t*2)*.65+rng.normal(0,1,len(t))*np.exp(-t*4)*.18+tone(74,1.8,'bell',.16))
t=np.arange(int(SR*1.1))/SR
save('telegraph',np.sin(2*np.pi*(210*t+70*t*t))*np.minimum(t*6,1)*np.exp(-t*3)*.13)
save('victory',sum(tone(n,2.4,'bell',.15) for n in [74,77,81,86]))

