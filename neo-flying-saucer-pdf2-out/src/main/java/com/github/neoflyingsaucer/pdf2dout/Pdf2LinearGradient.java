package com.github.neoflyingsaucer.pdf2dout;

import static com.github.neoflyingsaucer.pdf2dout.Pdf2PdfBoxWrapper.pdfAppendRawCommand;
import static com.github.neoflyingsaucer.pdf2dout.Pdf2PdfBoxWrapper.pdfCreatePatterns;
import static com.github.neoflyingsaucer.pdf2dout.Pdf2PdfBoxWrapper.pdfFillRect;
import static com.github.neoflyingsaucer.pdf2dout.Pdf2PdfBoxWrapper.pdfGetPatterns;
import static com.github.neoflyingsaucer.pdf2dout.Pdf2PdfBoxWrapper.pdfRestoreGraphics;
import static com.github.neoflyingsaucer.pdf2dout.Pdf2PdfBoxWrapper.pdfSaveGraphics;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDPatternResources;

import com.github.neoflyingsaucer.displaylist.DlInstruction.DlLinearGradient;
import com.github.neoflyingsaucer.displaylist.DlInstruction.DlStopPoint;
import com.github.pdfstream.PdfException;

public class Pdf2LinearGradient
{
	private final DlLinearGradient g;
	private final float x;
	private final float y;
	private final float w;
	private final float h;
	private final float dotsPerPoint;
	private boolean hasAlpha = false;
	private final DecimalFormat df = new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.US));
	private final int specialPatternCount;
	private final float opacity;
	
	public Pdf2LinearGradient(DlLinearGradient g, float x, float y, float w, float h, float dotsPerPoint, int specialPatternCount, float opacity)
	{
		this.g = g;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.dotsPerPoint = dotsPerPoint;
		this.specialPatternCount = specialPatternCount;
		this.opacity = opacity;
		
		if (opacity != 1f)
		{
			this.hasAlpha = true;
		}
		else
		{
			for (DlStopPoint sv : g.stopPoints)
			{
				if (sv.rgb.a != 255)
					this.hasAlpha = true;
			}
		}
	}
	
	public boolean hasAlpha()
	{
		return this.hasAlpha;
	}
	
	public void paint(PDPage page, PDPageContentStream strm, int nextGStateNumber, int lGradientObjNumber)
	{
        PDResources resources = page.findResources();
   		
        if (hasAlpha)
        {
        	PDExtendedGraphicsState alphaShader = addSpecialShader(g, x, y, w, h);
        	
        	Map<String, PDExtendedGraphicsState> gss = resources.getGraphicsStates();
        	
        	if (gss == null)
        		gss = new TreeMap<String, PDExtendedGraphicsState>();
        	
        	gss.put("MYGS" + nextGStateNumber, alphaShader);
        	resources.setGraphicsStates(gss);
        }
        
        COSArray functions = addLowerLevelFunctions(g, false);
        COSDictionary stitcher = createStitcherFunction(g, dotsPerPoint, functions);
        COSDictionary shading = createShadingDictionary(g, stitcher, x, y);
        
        Map<String, PDPatternResources> patterns = pdfGetPatterns(resources);
        if (patterns == null)
        	patterns = new TreeMap<String, PDPatternResources>();
        
        COSDictionary patternDictionary = new COSDictionary();
        patternDictionary.setItem(COSName.TYPE, COSName.PATTERN);
        patternDictionary.setItem(COSName.PATTERN_TYPE, COSInteger.TWO);
        patternDictionary.setItem(COSName.SHADING, shading);
        
        PDPatternResources patternResources = pdfCreatePatterns(patternDictionary);
        patterns.put("LGRADIENT" + lGradientObjNumber, patternResources);
        
        resources.setPatterns(patterns);

        pdfSaveGraphics(strm);

        if (hasAlpha)
        	pdfAppendRawCommand("/MYGS" + nextGStateNumber + " gs\n", strm);
        
        pdfAppendRawCommand("/Pattern cs\n", strm);
        pdfAppendRawCommand("/LGRADIENT" + lGradientObjNumber + " scn\n", strm);
        pdfFillRect(x, y, w, h, strm);

        pdfRestoreGraphics(strm);
	}
	
	private COSDictionary createStitcherFunction(DlLinearGradient g, float dotsPerPoint, COSArray shadingFunctions)
	{
        COSDictionary shadingDictionary = new COSDictionary();
        shadingDictionary.setItem(COSName.FUNCTION_TYPE, COSInteger.THREE);

        COSArray domainArray = new COSArray();
        domainArray.add(new COSFloat(0));
        domainArray.add(new COSFloat(1));
        shadingDictionary.setItem(COSName.DOMAIN, domainArray);
		
        float lastStopPosition = g.stopPoints.get(g.stopPoints.size() - 1).dots;
        
        COSArray bounds = new COSArray();
 		for (int i = 1; i < g.stopPoints.size() - 1;i++)
		{
			bounds.add(new COSFloat(g.stopPoints.get(i).dots / lastStopPosition));
		}
 		shadingDictionary.setItem(COSName.BOUNDS, bounds);
		
 		COSArray encoding = new COSArray();
		for (int i = 0; i < g.stopPoints.size() - 1;i++)
		{
			encoding.add(new COSFloat(0));
			encoding.add(new COSFloat(1));
		}
		shadingDictionary.setItem(COSName.ENCODE, encoding);
		
		shadingDictionary.setItem(COSName.FUNCTIONS, shadingFunctions);
		return shadingDictionary;
	}
	
    private COSArray addLowerLevelFunctions(DlLinearGradient g, boolean isOpacity)
    {
		COSArray array = new COSArray();
    	
    	for (int i = 0; i < g.stopPoints.size() - 1; i++)
		{
			COSDictionary obj = new COSDictionary();

			DlStopPoint sv = g.stopPoints.get(i);
			DlStopPoint nxt = g.stopPoints.get(i + 1);

			obj.setItem(COSName.FUNCTION_TYPE, COSInteger.TWO);
			obj.setItem(COSName.N, new COSFloat(1));
			
			COSArray domain = new COSArray();
			domain.add(new COSFloat(0));
			domain.add(new COSFloat(1));
			obj.setItem(COSName.DOMAIN, domain);

			if (!isOpacity)
			{
				// Color at start of function domain.
				COSArray startColor = new COSArray();
				startColor.add(new COSFloat(sv.rgb.r / 255f));
				startColor.add(new COSFloat(sv.rgb.g / 255f));
				startColor.add(new COSFloat(sv.rgb.b / 255f));
				obj.setItem(COSName.C0, startColor);
				
				// Color at end of function domain.
				COSArray endColor = new COSArray();
				endColor.add(new COSFloat(nxt.rgb.r / 255f));
				endColor.add(new COSFloat(nxt.rgb.g / 255f));
				endColor.add(new COSFloat(nxt.rgb.b / 255f));
				obj.setItem(COSName.C1, endColor);
			}
			else
			{
				COSArray startAlpha = new COSArray();
				startAlpha.add(new COSFloat(sv.rgb.a / 255f * opacity));
				
				COSArray endAlpha = new COSArray();
				endAlpha.add(new COSFloat(nxt.rgb.a / 255f * opacity));
				
				obj.setItem(COSName.C0, startAlpha);
				obj.setItem(COSName.C1, endAlpha);
			}

			array.add(obj);
		}

    	return array;
    }
    
    private COSDictionary createShadingDictionary(DlLinearGradient g, COSDictionary stitcher, float x, float y)
    {
    	COSDictionary dict = new COSDictionary();
 
   		dict.setItem(COSName.SHADING_TYPE, COSInteger.TWO);
   		dict.setItem(COSName.COLORSPACE, COSName.DEVICERGB);
   		
   		COSArray extend = new COSArray();
   		extend.add(COSBoolean.TRUE);
   		extend.add(COSBoolean.TRUE);
   		dict.setItem(COSName.EXTEND, extend);
   		
   		COSArray coords = new COSArray();
   		coords.add(new COSFloat((g.x1 + x * dotsPerPoint) / dotsPerPoint));
   		coords.add(new COSFloat((g.y2 + y * dotsPerPoint) / dotsPerPoint));
   		coords.add(new COSFloat((g.x2 + x * dotsPerPoint) / dotsPerPoint));
   		coords.add(new COSFloat((g.y1 + y * dotsPerPoint) / dotsPerPoint));
   		
   		dict.setItem(COSName.COORDS, coords);
   		dict.setItem(COSName.FUNCTION, stitcher);
   		
   		return dict;
    }
    
    private PDExtendedGraphicsState addSpecialShader(DlLinearGradient gradient, float x, float y, float w, float h)
    {
    	COSStream result = new COSStream(new RandomAccessBuffer());

    	COSArray bbox = new COSArray();
    	bbox.add(new COSFloat(0));
    	bbox.add(new COSFloat(0));
    	bbox.add(new COSFloat(x + w));
    	bbox.add(new COSFloat(y + h));

       	COSArray coords = new COSArray();
    	coords.add(new COSFloat((gradient.x1 + x)  / dotsPerPoint));
    	coords.add(new COSFloat((gradient.y2 + y) / dotsPerPoint));
    	coords.add(new COSFloat((gradient.x2 + x) / dotsPerPoint));
    	coords.add(new COSFloat((gradient.y1 + y) / dotsPerPoint));
    	
    	COSDictionary group = new COSDictionary();
    	group.setItem(COSName.CS, COSName.DEVICEGRAY);
    	group.setItem(COSName.S, COSName.getPDFName("Transparency"));
    	group.setItem(COSName.TYPE, COSName.getPDFName("Group"));

     	COSArray extend = new COSArray();
    	extend.add(COSBoolean.TRUE);
    	extend.add(COSBoolean.TRUE);

    	COSArray funcs = addLowerLevelFunctions(gradient, true);
    	COSDictionary stitcher = createStitcherFunction(gradient, dotsPerPoint, funcs);
    	
    	COSDictionary shading = new COSDictionary();
    	shading.setItem(COSName.COLORSPACE, COSName.DEVICEGRAY);
    	shading.setItem(COSName.SHADING_TYPE, COSInteger.TWO);
     	shading.setItem(COSName.EXTEND, extend);
     	shading.setItem(COSName.COORDS, coords);
     	shading.setItem(COSName.FUNCTION, stitcher);
    	
    	COSDictionary resource = new COSDictionary();
    	resource.setItem(COSName.TYPE, COSName.PATTERN);
    	resource.setItem(COSName.PATTERN_TYPE, COSInteger.TWO);
    	resource.setItem(COSName.SHADING, shading);
    	
    	COSDictionary resourceWrapper = new COSDictionary();
    	resourceWrapper.setItem("MYPATTERN" + specialPatternCount, resource);
    	
    	COSDictionary resourcesWrapper = new COSDictionary();
    	resourcesWrapper.setItem(COSName.PATTERN, resourceWrapper);
    	
    	result.setItem(COSName.BBOX, bbox);
    	result.setItem(COSName.FORMTYPE, COSInteger.ONE);
    	result.setItem(COSName.SUBTYPE, COSName.FORM);
    	result.setItem(COSName.TYPE, COSName.XOBJECT);
    	result.setItem("Group", group);
    	result.setItem(COSName.RESOURCES, resourcesWrapper);
    	
    	OutputStream strm = null;
		try {
			strm = result.createUnfilteredStream();
    	
			String strmContents =
				"q\n" +
				"/Pattern cs\n" +
				"/MYPATTERN" + specialPatternCount + " scn\n" +
				df.format(x) + " " + df.format(y) + " " + df.format(w) + " " + df.format(h) + " re f\nQ\n";
    		
			strm.write(strmContents.getBytes("windows-1252"));
			strm.close();
		} catch (IOException e) {
			throw new PdfException(e);
		}
		finally
		{
			if (strm != null)
				try {
					strm.close();
				} catch (IOException e) {
				}
		}
		
    	COSDictionary smask = new COSDictionary();
    	smask.setItem(COSName.S, COSName.getPDFName("Luminosity"));
    	smask.setItem(COSName.TYPE, COSName.MASK);
    	smask.setItem("G", result);
    	
    	COSDictionary extgstate = new COSDictionary();
    	extgstate.setItem(COSName.AIS, COSBoolean.FALSE);
    	extgstate.setItem(COSName.TYPE, COSName.EXT_G_STATE);
    	extgstate.setItem(COSName.SMASK, smask);
    	return new PDExtendedGraphicsState(extgstate);
    }
}
