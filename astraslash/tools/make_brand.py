"""Original orbital saber monogram; SVG masters plus Android raster exports."""
from pathlib import Path
import cairosvg
p=Path(__file__).resolve().parents[1]/'assets/art'
defs='''<defs><linearGradient id="blade" x1="0" y1="1" x2="1" y2="0"><stop stop-color="#39bade"/><stop offset=".6" stop-color="#9bfaff"/><stop offset="1" stop-color="#ffffff"/></linearGradient><linearGradient id="bg" x2="1" y2="1"><stop stop-color="#142c45"/><stop offset="1" stop-color="#050914"/></linearGradient></defs>'''
mark='''<g transform="translate(256 256)"><path d="M-91 80 L1-123 L75 51 L33 42 L-1-43 L-36 45 Z" fill="url(#blade)"/><path d="M-118 101 L111-105 L-22 70 Z" fill="#f7f3e7"/><path d="M20 92 L107 32 L89 78 L40 111 Z" fill="#f36259"/><path d="M-104-57 A119 119 0 0 1 106 59" fill="none" stroke="#cdb782" stroke-width="4"/><path d="M-93-75 L-104-57 L-87-58" fill="none" stroke="#cdb782" stroke-width="4"/><path d="M96-91 L100-79 L112-75 L100-71 L96-59 L92-71 L80-75 L92-79 Z" fill="#f57d66"/></g>'''
for name,body in [('icon', '<rect width="512" height="512" fill="url(#bg)"/>'+mark),('icon_foreground',mark),('icon_background','<rect width="512" height="512" fill="url(#bg)"/>'),('icon_monochrome',mark.replace('url(#blade)','#ffffff').replace('#f36259','#ffffff').replace('#cdb782','#ffffff').replace('#f57d66','#ffffff').replace('#f7f3e7','#ffffff'))]:
    svg=f'<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">{defs}{body}</svg>'
    (p/(name+'.svg')).write_text(svg)
    cairosvg.svg2png(bytestring=svg.encode(),write_to=str(p/(name+'.png')),output_width=512,output_height=512)
print('Brand masters and four Android icons generated.')
