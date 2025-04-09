from http.server import BaseHTTPRequestHandler, HTTPServer
import sys

class MyHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-type", "text/plain")
        self.end_headers()
        self.wfile.write(b"Hello! You sent a GET request.")

    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length)
        print("POST Body:", body.decode())

        self.send_response(200)
        self.send_header("Content-type", "application/json")
        self.end_headers()
        self.wfile.write(b'{"status": "POST received"}')

    def log_message(self, format, *args):
        # Suppress default logging
        return

def run_server(port):
    server_address = ('', port)
    httpd = HTTPServer(server_address, MyHandler)
    print(f"Server running at http://localhost:{port}")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down server.")
        httpd.server_close()

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python server.py <port>")
        sys.exit(1)

    try:
        port = int(sys.argv[1])
        run_server(port)
    except ValueError:
        print("Invalid port number.")
        sys.exit(1)
