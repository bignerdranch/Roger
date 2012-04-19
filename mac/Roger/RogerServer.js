var sys = require("util"),
net = require("net"),  
http = require("http"),  
path = require("path"),  
url = require("url"),  
filesys = require("fs");

port = 8081;
mobilePort = 8082;
hostname = process.argv[2];
var clients = [];

Array.prototype.remove = function(e) {
    for (var i = 0; i < this.length; i++) {
        if (e == this[i]) { return this.splice(i, 1); }
    }
};

http.createServer(function(request, response){  
                  var parts = url.parse(request.url, true);
                  sys.puts("got path: " + parts.pathname + " query: " + parts.query);
                  
                  if (parts.pathname == "/post") {
                  // Desktop client is posting a file
                  var apk = parts.query['apk'];
                  var layout = parts.query['layout'];
                  sys.puts("posting apk: " + apk + " layout: " + layout);
                  response.writeHeader(200);
                  
                  filesys.readFile(apk, "binary", function(err, file) {  
                                   if(!err) {        
                                   clients.forEach(function(s) {
                                                   sys.puts("sending data to a client");
                                                   s.write("--" + layout + "--", "binary");
                                                   s.write(file, "binary");
                                                   });  
                                   } else {
                                   sys.puts("unable to find file " + apk + " : " + err);
                                   }
                                   });  
                  } 
                  
                  response.end();
                  }).listen(port);  
sys.puts("Server Running on " + port);

var server = net.createServer(function (stream) {
                              clients.push(stream);
                              
                              stream.setTimeout(0);
                              stream.setEncoding("utf8");
                              
                              stream.addListener("connect", function () {
                                                 sys.puts("Added mobile client. Total: " + clients.length);
                                                 stream.pipe(stream);
                                                 });
                              
                              stream.addListener("end", function() {
                                                 clients.remove(stream);
                                                 sys.puts("Removed mobile client. Total: " + clients.length);
                                                 stream.end();
                                                 });
                              });
server.listen(mobilePort, hostname);
sys.puts("Mobile server Running on " + hostname + ":" + mobilePort);