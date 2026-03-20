import logging
import sys

def setup_logging():
    """
    Configure the root logger with a consistent format and level.
    This should be called once at the application entry point.
    """
    root_logger = logging.getLogger()
    root_logger.setLevel(logging.INFO)
    
    # Check if a handler already exists to avoid duplicate logs when reloading
    if not root_logger.handlers:
        console_handler = logging.StreamHandler(sys.stdout)
        # Using the user's preferred format: 'INFO:\t  message'
        formatter = logging.Formatter('%(levelname)s:\t  %(message)s')
        console_handler.setFormatter(formatter)
        root_logger.addHandler(console_handler)
        
    return root_logger

def get_logger(name: str):
    """
    Returns a logger for the given name.
    """
    return logging.getLogger(name)
