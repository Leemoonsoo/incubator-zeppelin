#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Script for installing R / Python dependencies for Travis CI
set -ev
touch ~/.environ

# Install R dependencies if SPARKR is true
if [[ "${SPARKR}" = "true" ]] ; then
  if [[ -n "$GITHUB_ACTION" ]]; then
    # github action
    sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys E298A3A825C0D65DFD57CBB651716619E084DAB9
    sudo add-apt-repository 'deb https://cloud.r-project.org/bin/linux/ubuntu bionic-cran35/'
  else
    # travis
    sudo echo "deb http://cran.rstudio.com/bin/linux/ubuntu xenial/" | sudo tee -a /etc/apt/sources.list
    sudo gpg --keyserver keyserver.ubuntu.com --recv-key E084DAB9
    sudo gpg -a --export E084DAB9 | sudo apt-key add -
  fi

  sudo apt-get -y update
  sudo apt-get -y --allow-unauthenticated install r-base r-base-dev

  echo "R_LIBS=~/R" > ~/.Renviron
  echo "export R_LIBS=~/R" >> ~/.environ
  source ~/.environ
  if [[ ! -d "$HOME/R/knitr" ]] ; then
    mkdir -p ~/R
    R -e "install.packages('evaluate', repos = 'http://cran.us.r-project.org', lib='~/R')"  > /dev/null 2>&1
    R -e "install.packages('base64enc', repos = 'http://cran.us.r-project.org', lib='~/R')"  > /dev/null 2>&1
    R -e "install.packages('knitr', repos = 'http://cran.us.r-project.org', lib='~/R')"  > /dev/null 2>&1
    R -e "install.packages('ggplot2', repos = 'http://cran.us.r-project.org', lib='~/R')"  > /dev/null 2>&1
  fi
fi

# Install Python dependencies for Python specific tests
if [[ -n "$PYTHON" ]] ; then
  wget https://repo.continuum.io/miniconda/Miniconda${PYTHON}-4.2.12-Linux-x86_64.sh -O miniconda.sh
  bash miniconda.sh -b -p $HOME/miniconda
  echo "export PATH='$HOME/miniconda/bin:$PATH'" >> ~/.environ
  source ~/.environ

  hash -r
  conda config --set always_yes yes --set changeps1 no
  conda update -q conda
  conda info -a
  conda config --add channels conda-forge

  conda install -q numpy=1.13.3 pandas=0.21.1 matplotlib=2.1.1 pandasql=0.7.3 ipython=5.4.1 jupyter_client=5.1.0 ipykernel=4.7.0 bokeh=0.12.10
  pip install -q scipy==0.18.0 ggplot==0.11.5 grpcio==1.8.2 bkzep==0.4.0

  if [[ -n "$TENSORFLOW" ]] ; then
    check_results=`conda search -c conda-forge tensorflow`
    echo "search tensorflow = $check_results"

    pip install tensorflow==${TENSORFLOW}
  fi
fi
