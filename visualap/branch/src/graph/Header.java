/*
Version 1.0, 30-12-2007, First release

IMPORTANT NOTICE, please read:

This software is licensed under the terms of the GNU GENERAL PUBLIC LICENSE,
please read the enclosed file license.txt or http://www.gnu.org/licenses/licenses.html

Note that this software is freeware and it is not designed, licensed or intended
for use in mission critical, life support and military purposes.

The use of this software is at the risk of the user.
*/

/* class Header

This class contains the header of visualap data files

javalc6
*/
package graph;
import java.util.HashMap;

public class Header extends HashMap<String, String> {

	public Header(String application, String comment, String version) {
		super();
		put("application", application);
		put("comment", comment);
		put("version", version);
	}

	public Header() {
		super();
	}
}
