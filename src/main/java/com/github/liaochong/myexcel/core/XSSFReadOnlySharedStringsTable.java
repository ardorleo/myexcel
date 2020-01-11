/*
 * Copyright 2019 liaochong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.liaochong.myexcel.core;

import com.github.liaochong.myexcel.core.cache.StringEhCache;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.apache.poi.xssf.usermodel.XSSFRelation.NS_SPREADSHEETML;

/**
 * @author liaochong
 * @version 1.0
 */
public class XSSFReadOnlySharedStringsTable extends DefaultHandler implements SharedStrings {
    /**
     * 内存最大存储最大值
     */
    private static final int MAX_IN_MEMORY = 5 * 1024 * 1024;

    protected final boolean includePhoneticRuns;

    /**
     * An integer representing the total count of strings in the workbook. This count does not
     * include any numbers, it counts only the total of text strings in the workbook.
     */
    protected int count;

    /**
     * An integer representing the total count of unique strings in the Shared String Table.
     * A string is unique even if it is a copy of another string, but has different formatting applied
     * at the character level.
     */
    protected int uniqueCount;
    /**
     * The shared strings table.
     */
    private Map<Integer, String> strings;
    /**
     * 当前数量
     */
    private int currentCount;
    /**
     * 内存中最大索引
     */
    private int memoryIndex;
    /**
     * 当前字符串大小
     */
    private int currentSize;

    private StringEhCache stringEhCache;

    /**
     * Calls {{@link #XSSFReadOnlySharedStringsTable(OPCPackage, boolean, StringEhCache)}} with
     * a value of <code>true</code> for including phonetic runs
     *
     * @param pkg The {@link OPCPackage} to use as basis for the shared-strings table.
     * @throws IOException  If reading the data from the package fails.
     * @throws SAXException if parsing the XML data fails.
     */
    public XSSFReadOnlySharedStringsTable(OPCPackage pkg, StringEhCache cache)
            throws IOException, SAXException {
        this(pkg, true, cache);
    }

    /**
     * @param pkg                 The {@link OPCPackage} to use as basis for the shared-strings table.
     * @param includePhoneticRuns whether or not to concatenate phoneticRuns onto the shared string
     * @throws IOException  If reading the data from the package fails.
     * @throws SAXException if parsing the XML data fails.
     * @since POI 3.14-Beta3
     */
    public XSSFReadOnlySharedStringsTable(OPCPackage pkg, boolean includePhoneticRuns, StringEhCache cache)
            throws IOException, SAXException {
        this.includePhoneticRuns = includePhoneticRuns;
        this.stringEhCache = cache;
        ArrayList<PackagePart> parts =
                pkg.getPartsByContentType(XSSFRelation.SHARED_STRINGS.getContentType());

        // Some workbooks have no shared strings table.
        if (parts.size() > 0) {
            PackagePart sstPart = parts.get(0);
            readFrom(sstPart.getInputStream());
        }
    }

    /**
     * Like POIXMLDocumentPart constructor
     * <p>
     * Calls {@link #XSSFReadOnlySharedStringsTable(PackagePart, boolean)}, with a
     * value of <code>true</code> to include phonetic runs.
     *
     * @since POI 3.14-Beta1
     */
    public XSSFReadOnlySharedStringsTable(PackagePart part) throws IOException, SAXException {
        this(part, true);
    }

    /**
     * Like POIXMLDocumentPart constructor
     *
     * @since POI 3.14-Beta3
     */
    public XSSFReadOnlySharedStringsTable(PackagePart part, boolean includePhoneticRuns)
            throws IOException, SAXException {
        this.includePhoneticRuns = includePhoneticRuns;
        readFrom(part.getInputStream());
    }

    /**
     * Read this shared strings table from an XML file.
     *
     * @param is The input stream containing the XML document.
     * @throws IOException  if an error occurs while reading.
     * @throws SAXException if parsing the XML data fails.
     */
    public void readFrom(InputStream is) throws IOException, SAXException {
        // test if the file is empty, otherwise parse it
        PushbackInputStream pis = new PushbackInputStream(is, 1);
        int emptyTest = pis.read();
        if (emptyTest > -1) {
            pis.unread(emptyTest);
            InputSource sheetSource = new InputSource(pis);
            try {
                XMLReader sheetParser = SAXHelper.newXMLReader();
                sheetParser.setContentHandler(this);
                sheetParser.parse(sheetSource);
            } catch (ParserConfigurationException e) {
                throw new RuntimeException("SAX parser appears to be broken - " + e.getMessage());
            }
        }
    }

    /**
     * Return an integer representing the total count of strings in the workbook. This count does not
     * include any numbers, it counts only the total of text strings in the workbook.
     *
     * @return the total count of strings in the workbook
     */
    @Override
    public int getCount() {
        return this.count;
    }

    /**
     * Returns an integer representing the total count of unique strings in the Shared String Table.
     * A string is unique even if it is a copy of another string, but has different formatting applied
     * at the character level.
     *
     * @return the total count of unique strings in the workbook
     */
    @Override
    public int getUniqueCount() {
        return this.uniqueCount;
    }


    @Override
    public RichTextString getItemAt(int idx) {
        return new XSSFRichTextString(memoryIndex == 0 || idx < memoryIndex ? strings.get(idx) : stringEhCache.get(idx));
    }

    //// ContentHandler methods ////

    private StringBuilder characters;
    private boolean tIsOpen;
    private boolean inRPh;

    @Override
    public void startElement(String uri, String localName, String name,
                             Attributes attributes) throws SAXException {
        if (uri != null && !uri.equals(NS_SPREADSHEETML)) {
            return;
        }

        if ("sst".equals(localName)) {
            String count = attributes.getValue("count");
            if (count != null) {
                this.count = Integer.parseInt(count);
            }
            String uniqueCount = attributes.getValue("uniqueCount");
            if (uniqueCount != null) {
                this.uniqueCount = Integer.parseInt(uniqueCount);
            }

            this.strings = new HashMap<>(1000);
            characters = new StringBuilder(64);
        } else if ("si".equals(localName)) {
            characters.setLength(0);
        } else if ("t".equals(localName)) {
            tIsOpen = true;
        } else if ("rPh".equals(localName)) {
            inRPh = true;
            //append space...this assumes that rPh always comes after regular <t>
            if (includePhoneticRuns && characters.length() > 0) {
                characters.append(" ");
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        if (uri != null && !uri.equals(NS_SPREADSHEETML)) {
            return;
        }

        if ("si".equals(localName)) {
            String content = characters.toString();
            currentSize += content.length() * 3;
            if (currentSize < MAX_IN_MEMORY) {
                strings.put(currentCount++, content);
            } else {
                if (memoryIndex == 0) {
                    memoryIndex = currentCount;
                }
                stringEhCache.cache(currentCount++, content);
            }
        } else if ("t".equals(localName)) {
            tIsOpen = false;
        } else if ("rPh".equals(localName)) {
            inRPh = false;
        }
    }

    /**
     * Captures characters only if a t(ext) element is open.
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (tIsOpen) {
            if (inRPh && includePhoneticRuns) {
                characters.append(ch, start, length);
            } else if (!inRPh) {
                characters.append(ch, start, length);
            }
        }
    }
}
