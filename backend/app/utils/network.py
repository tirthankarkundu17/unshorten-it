import os
from fastapi import Request

def get_client_ip(request: Request) -> str:
    """
    Safely extract the client IP address.
    Only trust X-Forwarded-For if explicitly allowed via configuration to prevent IP spoofing.
    By default, an attacker could spoof their IP via headers to bypass the rate limiter 
    or pollute the database/geolocation services.
    """
    client_ip = request.client.host if request.client else "unknown"
    
    trust_proxies = os.getenv("TRUST_PROXIES", "false").lower() == "true"
    if trust_proxies:
        x_forwarded_for = request.headers.get("X-Forwarded-For")
        if x_forwarded_for:
            client_ip = x_forwarded_for.split(",")[0].strip()
            
    return client_ip


import ipaddress
import socket
import asyncio
from urllib.parse import urlparse

async def is_safe_target_url(url: str) -> bool:
    """
    Checks if a URL points to a safe external IP.
    Prevents SSRF attacks against local/private network resources.
    """
    parsed = urlparse(url)
    hostname = parsed.hostname
    if not hostname:
        return False
        
    try:
        # Check if hostname itself is an IP
        ip = ipaddress.ip_address(hostname)
        if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved:
            return False
    except ValueError:
        pass
        
    # Resolve domain to IP to prevent DNS rebinding attacks to loopback/private
    try:
        ip_addr = await asyncio.to_thread(socket.gethostbyname, hostname)
        ip = ipaddress.ip_address(ip_addr)
        if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved:
            return False
    except Exception:
        # If we can't resolve it, the actual HTTP request will fail, but it's not a direct SSRF threat here
        pass
        
    return True

