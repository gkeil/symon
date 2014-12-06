
"C:\Users\Guille\Dropbox\Java\6502 Simulator Resources\cc65\bin\ca65" -o ehbasic.o min_mon.asm 

"C:\Users\Guille\Dropbox\Java\6502 Simulator Resources\cc65\bin\ld65" -C symon.config -vm -m ehbasic.map -o ehbasic.rom ehbasic.o 

