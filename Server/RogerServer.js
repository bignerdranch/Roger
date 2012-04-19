var sys = require("util"),
net = require("net"),  
http = require("http"),  
path = require("path"),  
url = require("url"),  
filesys = require("fs");

port = 8081;
mobilePort = 8082;
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
		var pack = parts.query['package'];
		sys.puts("posting apk: " + apk + " package: " + pack);
		response.writeHeader(200);
		
		clients.forEach(function(s) {
			sys.puts("sending data to a client");
			s.write("new data\n");
		});
		
	} 
	
	response.end();
    // var full_path = path.join(process.cwd(),my_path);  
    //     path.exists(full_path,function(exists){  
    //         if(!exists){  
    //             response.writeHeader(404, {"Content-Type": "text/plain"});  
    //             response.write("404 Not Found\n");  
    //             response.end();  
    //         }  
    //         else{  
    //             filesys.readFile(full_path, "binary", function(err, file) {  
    //                  if(err) {  
    //                      response.writeHeader(500, {"Content-Type": "text/plain"});  
    //                      response.write(err + "\n");  
    //                      response.end();    
    //   
    //                  }  
    //                  else{  
    //                     response.writeHeader(200);  
    //                     response.write(file, "binary");  
    //                     response.end();  
    //                 }  
    //   
    //             });  
    //         }  
    //     });  
}).listen(port);  
sys.puts("Server Running on " + port);

var server = net.createServer(function (stream) {
	clients.push(stream);

  	stream.setTimeout(0);
  	stream.setEncoding("utf8");

  	stream.addListener("connect", function () {
		sys.puts("Registering mobile client");
    	stream.write("Welcome\n");
  	});

  	stream.addListener("end", function() {
    	clients.remove(stream);
    	stream.end();
  	});
});
server.listen(mobilePort);
sys.puts("Mobile server Running on " + mobilePort);