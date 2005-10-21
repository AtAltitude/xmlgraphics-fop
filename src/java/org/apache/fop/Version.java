/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop;

/**
 * This class is used to evaluate the version information contained in the Manifest of FOP's JAR.
 * Note that this class can only find the version information if it's in the org.apache.fop package
 * as this package equals the one specified in the manifest.
 */
public class Version {

    /**
     * Get the version of FOP
     * @return the version string
     */
    public static String getVersion() {
        String version = Version.class.getPackage().getImplementationVersion();
        if (version == null) {
            //Fallback if FOP is used in a development environment
            String revision = "$LastChangedRevision$";
            if (revision.indexOf(":") >= 0) {
                revision = revision.substring(1, revision.length() - 2);
                revision = ", revision" + revision.substring(revision.lastIndexOf(" "));
            } else {
                revision = "";
            }
            version = "SVN Trunk" + revision;
        }
        return version;
    }
    
}
