#!/usr/bin/env python3
from http.server import HTTPServer, BaseHTTPRequestHandler
import os
from datetime import datetime

LOG_DIR = "app/apk_logs"
os.makedirs(LOG_DIR, exist_ok=True)

class LogHandler(BaseHTTPRequestHandler):
    def do_PUT(self):
        # 检查路由是否为 /apk_logs_receive
        if not self.path.startswith('/apk_logs_receive'):
            self.send_response(404)
            self.send_header('Content-Type', 'text/plain')
            self.end_headers()
            self.wfile.write(b'Not Found')
            return

        # 获取文件名（从 URL 路径提取，去掉 /apk_logs_receive 前缀）
        path_without_prefix = self.path[len('/apk_logs_receive'):]
        filename = os.path.basename(path_without_prefix)
        if not filename or filename == '/':
            filename = f"clawp_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"

        # 读取请求体（日志内容）
        content_length = int(self.headers.get('Content-Length', 0))
        log_content = self.rfile.read(content_length)

        # 保存到文件
        filepath = os.path.join(LOG_DIR, filename)
        with open(filepath, 'wb') as f:
            f.write(log_content)

        print(f"[{datetime.now()}] Received log: {filename} ({len(log_content)} bytes)")

        # 返回成功响应
        self.send_response(200)
        self.send_header('Content-Type', 'text/plain')
        self.end_headers()
        self.wfile.write(b'OK')

    def log_message(self, format, *args):
        # 自定义日志格式
        print(f"[{datetime.now()}] {format % args}")

if __name__ == '__main__':
    server = HTTPServer(('0.0.0.0', 50001), LogHandler)
    print(f"Log receiver started on http://0.0.0.0:50001")
    print(f"API endpoint: /apk_logs_receive")
    print(f"Logs will be saved to: {LOG_DIR}")
    server.serve_forever()
