var roboHydra = require("robohydra"),
	roboHydraHeads = roboHydra.heads,
	roboHydraHead = roboHydraHeads.RoboHydraHead;

RoboHydraHeadDAV = roboHydraHeads.roboHydraHeadType({
	name: 'WebDAV Server',
	mandatoryProperties: [ 'path' ],
    optionalProperties: [ 'handler' ],

	parentPropBuilder: function() {
		var myHandler = this.handler;
		return {
			path: this.path,
			handler: function(req,res,next) {
				// default DAV behavior
				res.headers['DAV'] = 'addressbook, calendar-access';
				res.statusCode = 500;

                // verify Accept header
                var accept = req.headers['accept'];
                if (req.method == "GET" && (accept == undefined || !accept.match(/text\/(calendar|vcard|xml)/)) ||
                    (req.method == "PROPFIND" || req.method == "REPORT") && (accept == undefined || accept != "text/xml"))
                    res.statusCode = 406;

				// DAV operations that work on all URLs
				else if (req.method == "OPTIONS") {
					res.statusCode = 204;
					res.headers['Allow'] = 'OPTIONS, PROPFIND, GET, PUT, DELETE, REPORT';

				} else if (req.method == "PROPFIND" && req.rawBody.toString().match(/current-user-principal/)) {
					res.statusCode = 207;
					res.write('\<?xml version="1.0" encoding="utf-8" ?>\
						<multistatus xmlns="DAV:">\
							<response>\
								<href>' + req.url + '</href> \
								<propstat>\
									<prop>\
										<current-user-principal>\
											<href>/dav/principals/users/test</href>\
										</current-user-principal>\
									</prop>\
									<status>HTTP/1.1 200 OK</status>\
								</propstat>\
							</response>\
						</multistatus>\
					');
					
				} else if (typeof myHandler != 'undefined')
					myHandler(req,res,next);

				res.end();
			}
		}
	}
});

module.exports = RoboHydraHeadDAV;
