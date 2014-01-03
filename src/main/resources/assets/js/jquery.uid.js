/**
* jquery.uid
* Generates a unique id with optional prefix/suffix.
* https://github.com/jstonne/jquery.uid
*
* Copyright (c) 2012 Jensen Tonne
* www.jstonne.com
*
* Dual licensed under the MIT and GPL licenses:
* http://www.opensource.org/licenses/mit-license.php
* http://www.gnu.org/licenses/gpl.html
*
*/

$.uid = function(p,s) {
	return ((p) ? p : '') + Math.random().toString().replace('.','') + ((s) ? s : '');
};

