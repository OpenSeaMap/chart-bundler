/*******************************************************************************
 * Copyright (c) OSMCB developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package osmcb.program.bundlecreators.KAPImages;

import static java.nio.file.StandardOpenOption.CREATE;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

import org.apache.log4j.Logger;

import osmb.exceptions.InvalidNameException;
import osmb.mapsources.MP2MapSpace;
import osmb.mapsources.TileAddress;
import osmb.program.ACApp;
import osmb.program.map.IfMap;
import osmb.program.tiles.Tile;
import osmb.program.tiles.Tile.TileState;
import osmb.utilities.OSMBStrs;
import osmcb.OSMCBSettings;
import osmcb.program.bundle.BundleTestException;
import osmcb.program.bundle.IfBundle;
import osmcb.program.bundle.MapCreationException;
import osmcb.program.bundlecreators.ACBundleCreator;
import osmcb.program.bundlecreators.IfBundleCreatorName;
import osmcb.utilities.OSMCBUtilities;
import osmcb.utilities.image.IfOSMPalette;
import osmcb.utilities.image.OSMAdaptivePalette;
import osmcb.utilities.image.OSMCB3AdaptivePalette;
import osmcb.utilities.image.OSMColor;

@IfBundleCreatorName(value = "OpenCPN KAP bundle", type = "OpenCPN")
// @SupportedTIParameters(names = {Name.format, Name.height, Name.width})
public class BCOpenCPN extends ACBundleCreator
{
	protected static final String STR_BUNDLE_TYPE = "OpenCPN-KAP";
	protected static final String STR_KAPDAT = "dd/MM/yyyy";
	protected static final String FILENAME_PATTERN = "t_%d_%d.%s";
	protected static final String LINEEND = "\r\n";

	public BCOpenCPN()
	{
		super();
		log = Logger.getLogger(this.getClass());
	}

	public BCOpenCPN(IfBundle bundle, File bundleOutputDir)
	{
		super();
		init(bundle, bundleOutputDir);
	}

	/**
	 * Creates a format specific directory for all OpenCPN-KAP bundles
	 * Creates a format specific directory name
	 */
	@Override
	public void initializeBundle() throws IOException, BundleTestException
	{
		log.trace(OSMBStrs.RStr("START"));
		File bundleOutputDir = ((OSMCBSettings) ACApp.getApp().getSettings()).getChartBundleOutputDirectory();
		bundleOutputDir = new File(bundleOutputDir, STR_BUNDLE_TYPE);
		OSMCBUtilities.mkDirs(bundleOutputDir);
		SimpleDateFormat sdf = new SimpleDateFormat(STR_BUFMT);
		mBundle.setBaseName("OSM-" + STR_BUNDLE_TYPE + "-" + mBundle.getName());
		String bundleDirName = mBundle.getBaseName() + "-" + sdf.format(new Date());
		bundleOutputDir = new File(bundleOutputDir, bundleDirName);
		super.initializeBundle(bundleOutputDir);
	}

	@Override
	public void finishBundle()
	{
		createInfoFile();
		sBundleProgress.finishBundle();
	}

	@Override
	public void createInfoFile()
	{
		createInfoFile("OpenSeaMap Charts KAP Bundle 0.2" + LINEEND);
	}

	@Override
	public void initializeMap() throws IOException
	{
		log.trace(OSMBStrs.RStr("START"));
		// each map goes in its own folder BUT all maps are in the same folder 'ChartBundleRoot'
		// NOAA has its own numbering scheme for the charts with more digits for smaller charts, but not a clear description to make subdivisions
		super.initializeMap();
		try
		{
			mMap.setName(mMap.getName().replace(" ", "M"));
		}
		catch (InvalidNameException e)
		{
			log.error("", e);
		}
	}

	/**
	 * put all tiles together in one file per map. this file is in .kap format
	 * 
	 * @throws InterruptedException
	 */
	@Override
	public void createMap() throws MapCreationException, InterruptedException
	{
		log.trace(OSMBStrs.RStr("START"));
		// the map is stored as a two file ensemble, one bsb file with a description and a kap file with the image data, so all tiles have to be put together
		try
		{
			writeKapFile();
			writeBsbFile();
		}
		catch (Exception e)
		{
			throw new MapCreationException(mMap, e);
		}
	}

	protected void writeBsbFile()
	{
		log.trace(OSMBStrs.RStr("START"));
		File bsbFile = new File(mOutputDir, mMap.getName() + ".bsb");
		// !
		// CRR/This electronic chart was produced under the authority of USA-NOAA/NOS.
		// By using this chart you acknowledge that you have read, understood
		// and accepted the terms and conditions stated in the User Agreement:
		// http://www.nauticalcharts.noaa.gov/mcd/Raster/download_agreement.htm
		// CHT/NA=NIIHAU TO FRENCH FRIGATE SHOALS,NU=19016
		// CHF/SAILING, INTERNATIONAL
		// CED/SE=12,RE=01,ED=04/01/2008
		// NTM/NE=12.145,ND=12/21/2013
		// VER/3.0
		// CHK/3,2767,2769,2768
		// CGD/14 -- no CGD outside us waters
		// ORG/USA-NOAA/NOS
		// MFR/USA-NOAA/NOS
		// RGN/40
		// K01/NA=NIIHAU TO FRENCH FRIGATE SHOALS,NU=2767,TY=BASE,FN=19016_1.KAP -- we have exactly one file in the map (LXX-MYYYY_1.KAP)
		// N000027670001/RT=N,KN=19016_1,CA=GENERAL,DE=RADAR REFLECTORS,P1=746,194,P2=1290,194,P3=1290,369,P4=746,369 -- atm no notes on charts, will follow maybe
		// someday

		//
		// above as taken from NOAA chart
		//
		// ! An example BSB text header
		// VER/3.0
		// CRR/2013, TeamSurv. All rights reserved.
		// CHT/NA=Australia 3000000, NU=123
		// CHF/Coastal
		// CED/SE=70,RE=01,ED=07/25/2012
		// NTM/NE=70.00,ND=07/25/2012, BF = on, BD=07/25/2012
		// CHK/1,123
		// ORG/TeamSurv -- OSeaM
		// MFR/TeamSurv -- OSeaM
		// CGD/5 -- no CGD outside us waters
		// RGN/4,6
		// K01/NA= Australia 3000000
		// NU=123
		// TY=Base
		// FN=123_1.KAP
		// N000005580027/RT=N,KN=12221_1,CA=CHART,DE=TIDE BOX,P1=3020,8412
		// P2=3020,8771,P3=4114,8771,P4=4114,8412
		// N000005580041/RT=L,KN=12221_1,LK=N000005580027,DE=TIDE BOX,
		// P1=8527, 707
		// The text header is terminated with a <Control-Z><NUL> sequence (ASCII characters 26 and 0).
		//
		// ! - Comment line
		// VER - Version number of BSB format � we will use 3.0
		// CRR - Copyright message. Free text
		// CHT - General parameters
		// NA - Chart name given to the BSB chart (can represent more than one .KAP)
		// NU - Chart number.
		// CHF Chart format (e.g. Overview, General, Coastal, Approach, River, Harbour or Berthing) -- L7,8,9 Overview, L10,11 General, L12, 13 Coastal, L14
		// Approach, L15,16 River, Harbour, Berthing
		// CED - Chart edition parameters - optional
		// SE - Source edition / number of paper chart
		// RE - Raster edition / number
		// ED - Chart edition date/number
		// NTM - Notices to mariners - optional
		// NE - NTM edition number
		// ND - NTM date
		// BF - Base flag on or off (I guess this allows a base edition of the chart to be specified?) It allows the chart to be updated with new ntms at a later
		// date
		// BD - Base date
		// CHK - number of KAPs, KAP-number [, KAP-number]
		// CGD - ? coast guard district � optional or can be unknown
		// RGN - ? - optional
		// ORG - Producing agency identifier -- OSeaM
		// MFR - Manufacturer of the RNC chart -- OSeaM
		// Knn -- we have exactly one file in the map (LXX-MYYYY_1.KAP)
		// NA - Chart name given to the pane
		// NU - Pane number e.g. 123_A
		// TY - Type. Base for the base chart, Inset for a plan contained within the base chart, or extension for a plan outside of the base area -- we only use
		// BASE maps
		// FN - KAP file name e.g. 123_1.kap
		// Naa � List of text boxes and hot spot links on the chart
		// RT - N for text box, L for hot spot link
		// KN - ?
		// CA - Category e.g. Chart, Caution, General etc
		// DE - Type e.g. Tide box, note etc
		// LK - Links this hot spot to the specified text box
		// P1, P2, P3, P4 � Hot spot location (just P1) or text box boundaries as 4 point polygon in x, y pixels
		//
		// ! An example BSB text header
		// VER/3.0
		// CRR/2014, OSeamM. All rights reserved.
		// CHT/NU=yyyy -- maybe names will follow someday
		// CHF/Coastal -- L7,8,9 Overview, L10,11 General, L12, 13 Coastal, L14 Approach, L15,16 River, Harbour, Berthing
		// CED/SE=70,RE=01,ED=07/25/2012
		// NTM/NE=70.00,ND=07/25/2012, BF = on, BD=07/25/2012
		// CHK/1,Lxx-Myyyy
		// ORG/OSeaM
		// MFR/OSeaM
		// -- no CGD outside us waters
		// RGN/4,6
		// K01/NA= Australia 3000000
		// NU=Myyyy
		// TY=Base
		// FN=Lxx-Myyyy.KAP
		// The text header is terminated with a <Control-Z><NUL> sequence (ASCII characters 26 and 0).
		//
		FileOutputStream bsbFileStream = null;

		try
		{
			log.trace("Writing bsb file");
			bsbFileStream = new FileOutputStream(bsbFile);

			OutputStreamWriter bsbWriter = new OutputStreamWriter(bsbFileStream, TEXT_FILE_CHARSET);
			String strDate = new SimpleDateFormat(STR_KAPDAT).format(new Date());

			bsbWriter.write("! - BSB File" + LINEEND);
			bsbWriter.write("VER/3.0" + LINEEND);
			bsbWriter.write("CRR/2016, OpenSeamMap. All rights reserved." + LINEEND);
			bsbWriter.write("CHT/NA=" + mMap.getName() + ",NU=" + mMap.getNumber() + LINEEND);
			bsbWriter.write("CHF/" + getCHF() + LINEEND);
			bsbWriter.write("CED/SE=1,RE=2,ED=" + strDate + LINEEND);
			bsbWriter.write("NTM/NE=70.00,ND=" + strDate + ",BF=on,BD=" + strDate + LINEEND);
			bsbWriter.write("CHK/1," + mMap.getName() + LINEEND);
			bsbWriter.write("ORG/OpenSeaMap" + LINEEND);
			bsbWriter.write("MFR/OpenSeaMap" + LINEEND);
			bsbWriter.write("CGD/unknown" + LINEEND);
			bsbWriter.write("RGN/unknown" + LINEEND);
			bsbWriter.write("K01/NA=" + mMap.getName() + ",NU=" + mMap.getNumber() + ",TY=BASE,FN=" + mMap.getName() + "_1.kap" + LINEEND);
			// bsbWriter.write("DTM/0,0\r\n");
			// bsbWriter.write("CPH/0\r\n");
			// bsbWriter.write("IFM/7\r\n");
			bsbWriter.flush();
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			OSMCBUtilities.closeStream(bsbFileStream);
		}
	}

	protected void writeKapFile() throws IOException
	{
		log.trace(OSMBStrs.RStr("START"));
		OutputStream mFS = null;

		Path mapFile = Files.createFile(mOutputDir.toPath().resolve(mMap.getName() + "_1.kap"));

		// The .kap file as used by OpenCPN is a text/binary combined file.
		// It consists of a BSB-header part and an image part (see misc/BSB-KAP Format.txt)
		try
		{
			BufferedImage img = createMapFromTiles();

			IfOSMPalette tPal = makePalette(img);
			mFS = Files.newOutputStream(mapFile, CREATE);

			log.debug("Writing map file (.kap)");

			ImageOutputStream ios = ImageIO.createImageOutputStream(mFS);
			writeMapHeader(mFS, tPal);

			long pos = Files.size(mapFile);

			writeMapImage(img, ios, tPal, pos);
			tPal = null;

			// log.debug("Writing test map file (.png)");
			// // these are here for testing purposes
			// File test = new File(mOutputDir, mMap.getName() + ".png");
			// ImageIO.write(img, "png", test);

			img = null;
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (MapCreationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			OSMCBUtilities.closeStream(mFS);
		}

		// File testTiff = new File(mapDir, map.getName() + ".tiff");
		// ImageIO.write(img, "tiff", testTiff);

		// File testTiff2 = new File(mapDir, map.getName() + "_2.tiff");
		// ImageIO.write(img, "tiff", testTiff2);
	}

	protected void writeMapHeader(OutputStream os, IfOSMPalette tPal) throws IOException
	{
		log.trace(OSMBStrs.RStr("START"));
		int tileSize = mMap.getTileSize().width;
		@SuppressWarnings("unused")
		int zoom = mMap.getZoom();
		OutputStreamWriter osw = new OutputStreamWriter(os, TEXT_FILE_CHARSET);

		double loMin = mMap.getMinLon();
		double loMax = mMap.getMaxLon();
		double laMin = mMap.getMinLat();
		double laMax = mMap.getMaxLat();

		log.debug("start writing image file for='" + mMap.getName() + "'");

		// BSB-header
		// VER/2.0
		// BSB/NA=NorthSea_GermanCoast12,NU=map.getName()
		// RA=12800,10240,DU=72
		// KNP/SC=508076,GD=WGS84,PR=MERCATOR,PP=UNKNOWN
		// PI=0.0,SP=,SK=0.0,TA=90.0
		// UN=METERS,SD=MEAN SEA LEVEL
		// DX=17.92,DY=17.92
		// CED/SE=2010,RE=01,ED=01/15/2010
		// OST/1
		// REF/1,0,0,55.0783672,4.5703125
		// REF/2,12800,10240,53.0147832,8.9648438
		// REF/3,12800,0,55.0783672,8.9648438
		// REF/4,0,10240,53.0147832,4.5703125
		// PLY/1,53.0147832,4.5703125
		// PLY/2,55.0783672,4.5703125
		// PLY/3,55.0783672,8.9648438
		// PLY/4,53.0147832,8.9648438
		// DTM/0,0
		// CPH/0
		// IFM/7
		// RGB/1,12,14,14
		// ...
		// RGB/127,220,214,237
		// DAY/
		// DSK/
		// NGT/
		// NGR/
		// GRY/
		// PRC/
		// PRG/
		// TIFF Image Data *

		// BSB (or NOS for older GEO/NOS or GEO/NO1 files) - General parameters
		// NA - Pane name
		// NU - Pane number. If chart is 123 and contains a plan A, the plan should be numbered 123_A
		// RA - width, height - width and height of raster image data in pixels
		// DU - Drawing Units in pixels/inch (same as DPI resolution) e.g. 50, 150, 175, 254, 300

		// KNP Detailed chart parameters
		// SC - Scale e.g. 25000 means 1:25000
		// GD - Geodetic Datum i.e. WGS84 for us
		// PR - Projection e.g. MERCATOR for us. Other known values are TRANSVERSE MERCATOR or LAMBERT CONFORMAL CONIC or POLYCONIC. This must be one of those
		// listed, as the value determines how PP etc. are interpreted. Only MERCATOR and TRANSVERSE MERCATOR are supported by OpenCPN.
		// PP - Projection parameter. For Mercator charts this is where the scale is valid - we use the average latitude of the chart.
		// For transverse Mercator it is the average longitude value.
		// PI - Projection interval ? e.g. 0.0, 0.033333, 0.083333, 2.0, UNKNOWN
		// SP - ? UNKNOWN is valid
		// SK - Skew angle i.e. 0.0 for us. Angle of rotation of the chart
		// TA - text angle i.e. 90 for us
		// UN - Depth units (for depths and heights) e.g. METRES, FATHOMS, FEET
		// SD - Sounding Datum e.g. MEAN LOWER LOW WATER, HHWLT or normally LAT
		// DU - Drawing Units in pixels/inch (same as DPI resolution) e.g. 50, 150, 175, 254, 300
		// DX,DY - Distance covered by one pixel [UN] i.e. meters

		// REF - start SW corner, proceed clockwise

		int width = (mMap.getXMax() - mMap.getXMin() + 1) * tileSize;
		int height = (mMap.getYMax() - mMap.getYMin() + 1) * tileSize;
		String strDate = new SimpleDateFormat(STR_KAPDAT).format(new Date());

		osw.write("! - KAP File" + LINEEND);
		osw.write("VER/3.0" + LINEEND);
		// Indentation !!!! osw.write("CRR/2015, OpenSeamMap. All rights reserved.\r\n " + createGeneralDisclaimer());
		osw.write("CRR/2016, OpenSeamMap. All rights reserved." + LINEEND);
		osw.write("CHT/NA=" + mMap.getName() + ",NU=" + mMap.getNumber() + LINEEND);
		osw.write("CHF/" + getCHF() + LINEEND);
		osw.write("CED/SE=1,RE=2,ED=" + strDate + LINEEND);
		osw.write("BSB/NA=" + mMap.getName() + ",NU=" + mMap.getNumber() + ",RA=" + width + "," + height + ",DU=220" + LINEEND);
		osw.write("ORG/OpenSeaMap" + LINEEND);
		osw.write("MFR/OpenSeaMap" + LINEEND);
		osw.write("KNP/PR=MERCATOR,GD=WGS84,SC=" + ((500000000) / Math.pow(2.0, mMap.getZoom())) + ",SD=LAT,UN=METRES,SK=0.0,TA=90.0,PI=UNKNOWN,SP=UNKNOWN,PP="
		    + (laMax - laMin) / 2 + LINEEND);
		osw.write("REF/1,0,0," + laMax + "," + loMin + LINEEND);
		osw.write("REF/2," + width + ",0," + laMax + "," + loMax + LINEEND);
		osw.write("REF/3," + width + "," + height + "," + laMin + "," + loMax + LINEEND);
		osw.write("REF/4,0," + height + "," + laMin + "," + loMin + LINEEND);
		osw.write("PLY/1," + laMax + "," + loMin + LINEEND);
		osw.write("PLY/2," + laMax + "," + loMax + LINEEND);
		osw.write("PLY/3," + laMin + "," + loMax + LINEEND);
		osw.write("PLY/4," + laMin + "," + loMin + LINEEND);
		osw.write("DTM/0.0,0.0" + LINEEND);
		osw.write("CPH/0.0" + LINEEND);
		osw.write("OST/1" + LINEEND);
		osw.write("IFM/7" + LINEEND);
		// two variants are possible
		// - we use a fixed colortable and match the pixels against the predefined color (quick and the same look in all charts)
		// - we create a new colortable for each map by some sophisticated algorithm (technically preferred, currently used)
		osw.write(tPal.asBSBStr());
		osw.flush();
		// write the separator before the image description
		os.write(0x1A);
		os.write(0x00);
		tPal = null;
	}

	protected String getCHF()
	{
		String strCHF = null;
		switch (mMap.getLayer().getZoomLvl())
		{
			case 8:
			case 9:
				strCHF = "Overview";
				break;
			case 12:
			case 13:
				strCHF = "Coastal";
				break;
			case 14:
			case 15:
				strCHF = "Approach";
				break;
			case 16:
			case 17:
				strCHF = "Harbour";
				break;
			case 18:
			case 19:
				strCHF = "Berthing";
				break;
			default:
				strCHF = "General";
				break;
		}
		return strCHF;
	}

	protected IfOSMPalette makePalette(BufferedImage img)
	{
		log.trace("START");
		OSMCB3AdaptivePalette tPal = new OSMCB3AdaptivePalette(img);
		// OSMFixedHSLPalette tPal = new OSMFixedHSLPalette(img);
		// log.debug("final Palette:" + tPal.toString());
		return tPal;
	}

	/**
	 * This writes a map image to the kap-file. The kap image description consists of the bits per pixel info, the scan lines and a scan line index
	 * 
	 * @param img
	 *          The maps image.
	 * @param ios
	 *          The output stream the data are written to.
	 * @param tPal
	 *          The palette to be used.
	 * @param nPos
	 *          The starting point in the final file, used in the scan line index table.
	 */
	protected void writeMapImage(BufferedImage img, ImageOutputStream ios, IfOSMPalette tPal, long nPos)
	{
		log.trace(OSMBStrs.RStr("START"));
		int nErrCnt = 0;
		ArrayList<Long> tLIdx = new ArrayList<>(img.getHeight());
		try
		{
			// write the bits per color (currently fixed to 7 - meaning we have up to 127 color in the palette. Unused colors may be omitted)
			ios.write(7);

			// Write the image pixel by pixel. we use rle (see ...).
			// The line numbers start with 1 not 0 according to libbsb for V2 and earlier, with 0 according to OpenCPN for V3 and later.
			// As of 2016-01-22 info by OpenCPN Dave aka bdbcat OpenCPN expects index 0 as the starting line.
			// for (int nY = 1; nY <= img.getHeight(); nY++)
			for (int nY = 0; nY < img.getHeight(); nY++)
			{
				// write the line index, we get the offset of the first scan line in the file from the outside by file.size() after writeMapHeader()
				// know no way to ask ios about that
				tLIdx.add(ios.getStreamPosition() + nPos);
				if (nY > 0x1FFFFF)
				{
					ios.write(((nY >> 21) & 0x7F) | 0x80);
				}
				if (nY > 0x3FFF)
				{
					ios.write(((nY >> 14) & 0x7F) | 0x80);
				}
				if (nY > 0x7F)
				{
					ios.write(((nY >> 7) & 0x7F) | 0x80);
				}
				ios.write((nY) & 0x7F);
				// now look for encodable runs
				for (int nX = 0; nX < img.getWidth(); nX++)
				{
					int nCnt = 1;
					// int nPalIdx = tPal.getPID(new OSMColor(img.getRGB(nX, nY - 1)));
					int nPalIdx = tPal.getPIdx(new OSMColor(img.getRGB(nX, nY)));

					// should compare the mapped colors. The current pixel and the next one.
					// while ((nX < img.getWidth() - 1) && (img.getRGB(nX + 1, nY - 1) == tCol.getRGB()))
					// while ((nX < img.getWidth() - 1) && (tPal.getPID(new OSMColor(img.getRGB(nX + 1, nY - 1))) == nPalIdx))
					while ((nX < img.getWidth() - 1) && (tPal.getPIdx(new OSMColor(img.getRGB(nX + 1, nY))) == nPalIdx))
					{
						nCnt++;
						nX++;
					}

					if (nPalIdx > 127)
					{
						// log.error(mMap.getName() + " [" + nX + "|" + (nY - 1) + "], (" + new OSMColor(img.getRGB(nX, nY - 1)).toStringRGB() + "), " + nCnt
						log.error(mMap.getName() + " [" + nX + "|" + (nY) + "], (" + new OSMColor(img.getRGB(nX, nY)).toStringRGB() + "), " + nCnt
						    + ", palette index wrong=" + nPalIdx + ", used=" + (nPalIdx & 0x7F) + ", errors=" + nErrCnt);
						++nErrCnt;
					}
					if ((false) && (nPalIdx == 0))
					{
						// log.error(mMap.getName() + " [" + nX + "|" + (nY - 1) + "], (" + new OSMColor(img.getRGB(nX, nY - 1)).toStringRGB() + "), " + nCnt
						log.error(mMap.getName() + " [" + nX + "|" + (nY) + "], (" + new OSMColor(img.getRGB(nX, nY)).toStringRGB() + "), " + nCnt
						    + ", palette index wrong=" + nPalIdx + ", used=" + (1) + ", errors=" + nErrCnt);
						nPalIdx = 1;
						++nErrCnt;
					}
					// for our 7bit palette the whole first byte is used by the color index, so the count will follow in the next byte -> set bit 7
					ios.write((nPalIdx & 0x7F) | 0x80);
					// calculate the run length
					nCnt--;
					if (nCnt > 0x1FFFFF)
					{
						ios.write(((nCnt >> 21) & 0x7F) | 0x80);
					}
					if (nCnt > 0x3FFF)
					{
						ios.write(((nCnt >> 14) & 0x7F) | 0x80);
					}
					if (nCnt > 0x7F)
					{
						ios.write(((nCnt >> 7) & 0x7F) | 0x80);
					}
					ios.write((nCnt) & 0x7F);
				}
				// write the line end marker
				ios.write(0);
			}
			// write the image data end marker
			ios.writeInt(0);
			// store the start of line index position into the table as the index of the last line ???? This is not neccessary for OpenCPN, but all libbsb based
			// programs need it.
			tLIdx.add(ios.getStreamPosition() + nPos);

			// write the line offset index table. we use the tLIdx ArrayList instead of the original image lines to address.
			// the last entry in the line index is the start of the line index itself.
			// This can skip line 0, the we get an 'unindexed' entry with 0000 at the start of the line index.
			for (int nY = 0; nY < tLIdx.size(); nY++)
			{
				long nIdx = tLIdx.get(nY);
				ios.write(((int) nIdx & 0xFF000000) >> 24);
				ios.write(((int) nIdx & 0x00FF0000) >> 16);
				ios.write(((int) nIdx & 0x0000FF00) >> 8);
				ios.write(((int) nIdx & 0x000000FF) >> 0);
			}
			// adjusted for 'line index start' entry as last entry in the line index
			log.debug("finished writing line index with " + (tLIdx.size() - 1) + " lines");
			ios.close();
			log.debug("finished writing image file for='" + mMap.getName() + "'");
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This writes a test map image to the kap-file.
	 * It creates a two colored square with a diagonal division.
	 */
	protected void writeTestMapImage(IfMap map, BufferedImage img, ImageOutputStream ios, OSMAdaptivePalette tPal, long nPos)
	{
		log.trace(OSMBStrs.RStr("START"));
		ArrayList<Long> tLIdx = new ArrayList<>(img.getHeight());
		try
		{
			// write the bits per color (currently fixed to 7 - meaning we have 127 color in the palette)
			ios.write(7);

			// write the image pixel by pixel we use rle (see ...)
			// the line numbers start with 1 not 0
			for (int nY = 1; nY <= img.getHeight(); nY++)
			{
				// write the line index, we get the offset of the first scan line in the file from the outside by file.size() after writeMapHeader()
				// know no way to ask ios about that
				tLIdx.add(ios.getStreamPosition() + nPos);
				if (nY > 0x1FFFFF)
				{
					ios.write(((nY >> 21) & 0x7F) | 0x80);
				}
				if (nY > 0x3FFF)
				{
					ios.write(((nY >> 14) & 0x7F) | 0x80);
				}
				if (nY > 0x7F)
				{
					ios.write(((nY >> 7) & 0x7F) | 0x80);
				}
				ios.write((nY) & 0x7F);

				// now write encoded runs
				{
					int nCnt = nY - 1;
					// for our 7bit palette the whole first Byte is used by the color index, so the count will follow -> set bit 7
					ios.write((1 & 0x7F) | 0x80);
					// calculate the run count
					if (nCnt > 0x1FFFFF)
					{
						ios.write(((nCnt >> 21) & 0x7F) | 0x80);
					}
					if (nCnt > 0x3FFF)
					{
						ios.write(((nCnt >> 14) & 0x7F) | 0x80);
					}
					if (nCnt > 0x7F)
					{
						ios.write((((nCnt >> 7)) & 0x7F) | 0x80);
					}
					ios.write((nCnt) & 0x7F);

					nCnt = img.getWidth() - nY - 1;
					// for our 7bit palette the whole first Byte is used by the color index, so the count will follow -> set bit 7
					ios.write((2 & 0x7F) | 0x80);
					// calculate the run count
					if (nCnt > 0x1FFFFF)
					{
						ios.write(((nCnt >> 21) & 0x7F) | 0x80);
					}
					if (nCnt > 0x3FFF)
					{
						ios.write(((nCnt >> 14) & 0x7F) | 0x80);
					}
					if (nCnt > 0x7F)
					{
						ios.write((((nCnt >> 7)) & 0x7F) | 0x80);
					}
					ios.write((nCnt) & 0x7F);
				}
				// write the line end marker
				ios.write(0);
			}
			ios.writeInt(0);

			// write the line offset index table
			for (int nY = 0; nY < img.getHeight(); nY++)
			{
				long nIdx = tLIdx.get(nY);
				ios.write(((int) nIdx & 0xFF000000) >> 24);
				ios.write(((int) nIdx & 0xFF0000) >> 16);
				ios.write(((int) nIdx & 0xFF00) >> 8);
				ios.write(((int) nIdx & 0xFF) >> 0);
			}
			ios.close();
			log.info("finished writing image file for='" + map.getName() + "'");
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected BufferedImage createMapFromTiles() throws InterruptedException, MapCreationException
	{
		log.trace(OSMBStrs.RStr("START"));
		int width = (mMap.getXMax() - mMap.getXMin() + 1) * MP2MapSpace.TECH_TILESIZE;
		int height = (mMap.getYMax() - mMap.getYMin() + 1) * MP2MapSpace.TECH_TILESIZE;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gc = img.createGraphics();
		int tilex = 0;
		int tiley = 0;

		ImageIO.setUseCache(false);
		// byte[] emptyTileData = OSMCBUtilities.createEmptyTileData(mapSource);
		// String tileType = mapSource.getTileImageType().getFileExt();
		for (int x = mMap.getXMin(); x <= mMap.getXMax(); x++)
		{
			tiley = 0;
			for (int y = mMap.getYMin(); y <= mMap.getYMax(); y++)
			{
				BufferedImage tileImage = null;
				Tile tile = null;
				TileAddress tAddr = new TileAddress(x, y, mMap.getZoom());
				// try to get the tile from the mtc
				if ((tile = sTC.getTile(mMap.getMapSource(), tAddr)) != null)
				{
					if (tile.getTileState() == TileState.TS_LOADING)
						log.warn("tried to load loading tile from mtc" + tile);
					else
						tileImage = tile.getImage();
				}
				if (tileImage == null)
				{
					// if the tile is not available in the mtc, get it from the tile store
					tile = mMap.getMapSource().getNTileStore().getTile(tAddr);
					if (tile.getTileState() == TileState.TS_LOADING)
						log.warn("tried to load loading tile from tile store" + tile);
					else
						tileImage = tile.getImage();
				}
				if (tileImage != null)
				{
					log.trace(String.format("Tile x=%d y=%d ", tilex, tiley));
					gc.drawImage(tileImage, tilex * MP2MapSpace.TECH_TILESIZE, tiley * MP2MapSpace.TECH_TILESIZE, MP2MapSpace.TECH_TILESIZE, MP2MapSpace.TECH_TILESIZE,
					    null);
				}
				else
				{
					log.warn(String.format("Tile x=%d y=%d not found in tile archive - creating error tile", tilex, tiley));
					tile = new Tile(mMap.getMapSource(), tilex, tiley, mMap.getZoom());
					tile.setErrorImage();
					gc.drawImage(tile.getImage(), tilex * MP2MapSpace.TECH_TILESIZE, tiley * MP2MapSpace.TECH_TILESIZE, MP2MapSpace.TECH_TILESIZE,
					    MP2MapSpace.TECH_TILESIZE, null);
				}
				tiley++;
			}
			tilex++;
		}
		return img;
	}

	// FileTileWriter deleted from BCOpenCPN/KAP, it is not usable here.
}
