/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package github.daneren2005.dsub.service.parser;

import android.content.Context;
import android.util.Log;
import github.daneren2005.dsub.R;
import github.daneren2005.dsub.domain.MusicDirectory;
import github.daneren2005.dsub.util.Constants;
import github.daneren2005.dsub.util.ProgressListener;
import github.daneren2005.dsub.util.Util;
import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;

/**
 * @author Sindre Mehus
 */
public class MusicDirectoryParser extends MusicDirectoryEntryParser {

    private static final String TAG = MusicDirectoryParser.class.getSimpleName();
	private Context context;

    public MusicDirectoryParser(Context context) {
        super(context);
		this.context = context;
    }

    public MusicDirectory parse(String artist, Reader reader, ProgressListener progressListener) throws Exception {
        long t0 = System.currentTimeMillis();
        init(reader);

        MusicDirectory dir = new MusicDirectory();
        int eventType;
		boolean isArtist = false;
        do {
            eventType = nextParseEvent();
            if (eventType == XmlPullParser.START_TAG) {
                String name = getElementName();
                if ("child".equals(name) || "song".equals(name) || "video".equals(name)) {
					MusicDirectory.Entry entry = parseEntry(artist);
					entry.setGrandParent(dir.getParent());
                    dir.addChild(entry);
                } else if ("directory".equals(name) || "artist".equals(name) || ("album".equals(name) && !isArtist)) {
                    dir.setName(get("name"));
					dir.setId(get("id"));
					dir.setParent(get("parent"));
					isArtist = true;
                } else if("album".equals(name)) {
					MusicDirectory.Entry entry = parseEntry(artist);
					entry.setDirectory(true);
					dir.addChild(entry);
				} else if ("error".equals(name)) {
                    handleError();
                }
            }
        } while (eventType != XmlPullParser.END_DOCUMENT);

        validate();
		
		// Only apply sorting on server version 4.7 and greater, where disc is supported
		if(Util.checkServerVersion(context, "1.8.0") && Util.getPreferences(context).getBoolean(Constants.PREFERENCES_KEY_CUSTOM_SORT_ENABLED, true)) {
			dir.sortChildren();
		}

        long t1 = System.currentTimeMillis();
        Log.d(TAG, "Got music directory in " + (t1 - t0) + "ms.");

        return dir;
    }
}
