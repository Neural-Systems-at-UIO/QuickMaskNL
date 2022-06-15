package png;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class PngWriter
{
	private final OutputStream os;
	private final ByteArrayOutputStream baos;
	private final DeflaterOutputStream dos;
	
	private int lines;
	public final int lines(){return lines;}
	
    /*
     * Greyscale				0
     * Truecolour				2
     * Indexed-colour			3
     * Greyscale with alpha		4
     * Truecolour with alpha	6
     */
	public static final byte TYPE_GRAYSCALE=0;
	public static final byte TYPE_TRUECOLOR=2;
	public static final byte TYPE_PALETTIZED=3;
	public static final byte TYPE_GRAYSCALE_ALPHA=4;
	public static final byte TYPE_TRUECOLOR_ALPHA=6;
	
	public PngWriter(OutputStream aStream,int aWidth,int aHeight,byte aType,byte aPalette[]) throws IOException
	{
		os=aStream;
		
		byte pngIdBytes[] = {-119, 80, 78, 71, 13, 10, 26, 10}; 
        os.write(pngIdBytes);
        
        byte pngHdrLengthBytes[] = {0,0,0,13};
        os.write(pngHdrLengthBytes);
        byte pngHdrBytes[]={73, 72, 68, 82, // IHDR
        		(byte)(aWidth >> 24),(byte)(aWidth >> 16),(byte)(aWidth >> 8),(byte)aWidth,
        		(byte)(aHeight >> 24),(byte)(aHeight >> 16),(byte)(aHeight >> 8),(byte)aHeight,
        		8, aType, 0, 0, 0}; // 8 bit per channel, type, standard filtering, no interlace
        os.write(pngHdrBytes);
        
        CRC32 crc=new CRC32();
        crc.update(pngHdrBytes);
        long crcval=crc.getValue();
        
        os.write((byte)(crcval >> 24));
        os.write((byte)(crcval >> 16));
        os.write((byte)(crcval >> 8));
        os.write((byte)crcval);
        
        if(aPalette!=null)
        {
        	int plength=aPalette.length;
	        os.write((byte)(plength >> 24));
	        os.write((byte)(plength >> 16));
	        os.write((byte)(plength >> 8));
	        os.write((byte)plength);
	        
	        byte PLTE[] = {80, 76, 84, 69};
	        os.write(PLTE);
	        os.write(aPalette);
	        
	        crc.reset();
	        crc.update(PLTE);
	        crc.update(aPalette);
	        crcval=crc.getValue();
	        
	        os.write((byte)(crcval >> 24));
	        os.write((byte)(crcval >> 16));
	        os.write((byte)(crcval >> 8));
	        os.write((byte)crcval);
        }
        
        baos=new ByteArrayOutputStream(aWidth*aHeight*3);
        byte IDAT[] = {73, 68, 65, 84}; // IDAT
        baos.write(IDAT);
        
//		Deflater d=new Deflater(9,true); // ActionScript
		Deflater d=new Deflater(9,false); // PNG
		dos=new DeflaterOutputStream(baos, d, aWidth*aHeight*3);
		
		lines=aHeight;
	}
	
	public void writeline(byte aLine[]) throws IOException
	{
		if(lines==0)throw new IOException("Surplus PNG line");
		dos.write(0);
		dos.write(aLine);
		if(--lines==0)
		{
			dos.finish();
			
			byte data[]=baos.toByteArray();
			int datalen=data.length-4;
	        os.write((byte)(datalen >> 24));
	        os.write((byte)(datalen >> 16));
	        os.write((byte)(datalen >> 8));
	        os.write((byte)datalen);
	        os.write(data);
			
	        CRC32 crc=new CRC32();
			crc.update(data);
	        long crcval=crc.getValue();
			
	        os.write((byte)(crcval >> 24));
	        os.write((byte)(crcval >> 16));
	        os.write((byte)(crcval >> 8));
	        os.write((byte)crcval);
	        
	        
	        os.write(0);
	        os.write(0);
	        os.write(0);
	        os.write(0);
	        byte IEND[] = {73, 69, 78, 68};
	        os.write(IEND);
	        
	        crc.reset();
	        crc.update(IEND);
	        crcval = crc.getValue();
	 	        
	        os.write((byte)(crcval >> 24));
	        os.write((byte)(crcval >> 16));
	        os.write((byte)(crcval >> 8));
	        os.write((byte)crcval);
	        
	        os.flush();
		}
	}
}
