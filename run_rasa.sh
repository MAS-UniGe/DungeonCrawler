#!/bin/bash

# Check if the virtual environment exists
if [ ! -d "rasa_env" ]; then
  echo "Virtual environment 'rasa_env' not found. Creating one with Python 3.10 and installing Rasa..."
  
  # Create the virtual environment with Python 3.10
  python3.10 -m venv rasa_env
  
  # Activate the virtual environment
  source rasa_env/bin/activate
  
  # Upgrade pip to the latest version
  pip install --upgrade pip
  
  # Install Rasa in the virtual environment
  pip install rasa
  
  echo "Virtual environment 'rasa_env' created and Rasa installed."
else
  # Activate the existing virtual environment
  source rasa_env/bin/activate
fi

# Check if the `src` directory exists
if [ ! -d "src" ]; then
  echo "Error: 'src' directory not found."
  exit 1
fi

# Navigate to the `src` directory
cd src || exit

# Run the Rasa application with the API enabled
rasa run --enable-api
