/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package com.kapx.ucms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.metadata.TikaAudioMetadataExtracter;
import org.alfresco.repo.content.metadata.TikaPoweredMetadataExtracter;
import org.alfresco.repo.content.metadata.TikaSpringConfiguredMetadataExtracter;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.video.FLVParser;

/**
 * Extracts the following values from FLV files: *
 * 
 * Note - XMPDM metadata keys are also emitted, in common with
 *  the other Tika powered extracters 
 * 
 * Uses Apache Tika
 * 
 * @author Kalpesh Mehta
 */
public class FLVMetadataExtracter extends TikaPoweredMetadataExtracter
{
    private static final String KEY_DURATION = "duration";
    private static final String KEY_TITLE = "title";
    protected TikaConfig tikaConfig;
    
    public void setTikaConfig(TikaConfig tikaConfig){
       this.tikaConfig = tikaConfig;
    }

    public static ArrayList<String> SUPPORTED_MIMETYPES = buildSupportedMimetypes(
          new String[] { MimetypeMap.MIMETYPE_VIDEO_FLV },
          new FLVParser()
    );
    
    public FLVMetadataExtracter()
    {
        super(SUPPORTED_MIMETYPES);
    }
    
    @Override
    protected Parser getParser() 
    {
       return new FLVParser();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Map<String, Serializable> extractSpecific(Metadata metadata,
         Map<String, Serializable> properties, Map<String,String> headers) {
       // Do the normal Audio mappings
       //super.extractSpecific(metadata, properties, headers);     
       System.out.println("FLV Duration:"+ metadata.get("duration"));
       System.out.println("FLV XMP Duration:"+ metadata.get(XMPDM.DURATION));
       putRawValue(KEY_DURATION, metadata.get("duration"), properties);       
       // All done
       return properties;
    }
}