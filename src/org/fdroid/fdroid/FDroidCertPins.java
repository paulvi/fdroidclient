/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid;

import java.util.ArrayList;
import java.util.Arrays;

public class FDroidCertPins {
    public static final String[] DEFAULT_PINS = {

            // OU=PositiveSSL, CN=f-droid.org
            // Fingerprint: 84B91CDF2312CB9BA7F3BE803783302F8D8C299F
            "638F93856E1F5EDFCBD40C46D4160CFF21B0713A",

            // OU=PositiveSSL, CN=f-droid.org
            "83a288fdbf7fb27ca2268d553168eb8f38298910",

            // OU=Gandi Standard SSL, CN=guardianproject.info
            "cf2f8e226027599a1a933701418c58ec688a8305",

            // C=US, ST=Washington, L=Seattle, O=Amazon.com Inc., CN=s3.amazonaws.com
            "5e77905babb66ca7082979435afbe4edf3f5af12",

            // OU=Domain Control Validated - RapidSSL(R), CN=www.psiphon.ca
            "3aa1726e64d54bf58bf68fe23208928fd0d9cf8a",

            // OU=EssentialSSL Wildcard, CN=*.panicbutton.io
            "cdae8cc70af09a55a7642d13f84241cba1c3a3e6",
    };

    public static ArrayList<String> PINLIST = null;

    public static String[] getPinList() {
        if (PINLIST == null) {
            ArrayList<String> pinlist = new ArrayList<String>();
            pinlist.addAll(Arrays.asList(DEFAULT_PINS));
            PINLIST = pinlist;
        }

        return PINLIST.toArray(new String[PINLIST.size()]);
    }
}
