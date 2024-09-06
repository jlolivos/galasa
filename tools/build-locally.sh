#!/usr/bin/env bash

#
# Copyright contributors to the Galasa project
#
# SPDX-License-Identifier: EPL-2.0
#

#-----------------------------------------------------------------------------------------                   
#
# Objectives: Build this repository code locally.
# 
#-----------------------------------------------------------------------------------------                   

# Where is this script executing from ?
BASEDIR=$(dirname "$0");pushd $BASEDIR 2>&1 >> /dev/null ;BASEDIR=$(pwd);popd 2>&1 >> /dev/null
# echo "Running from directory ${BASEDIR}"
export ORIGINAL_DIR=$(pwd)
# cd "${BASEDIR}"

cd "${BASEDIR}/.."
PROJECT_DIR=$(pwd)

#-----------------------------------------------------------------------------------------                   
#
# Set Colors
#
#-----------------------------------------------------------------------------------------                   
bold=$(tput bold)
underline=$(tput sgr 0 1)
reset=$(tput sgr0)
red=$(tput setaf 1)
green=$(tput setaf 76)
white=$(tput setaf 7)
tan=$(tput setaf 202)
blue=$(tput setaf 25)

#-----------------------------------------------------------------------------------------                   
#
# Headers and Logging
#
#-----------------------------------------------------------------------------------------                   
underline() { printf "${underline}${bold}%s${reset}\n" "$@" ; }
h1() { printf "\n${underline}${bold}${blue}%s${reset}\n" "$@" ; }
h2() { printf "\n${underline}${bold}${white}%s${reset}\n" "$@" ; }
debug() { printf "${white}[.] %s${reset}\n" "$@" ; }
info()  { printf "${white}[➜] %s${reset}\n" "$@" ; }
success() { printf "${white}[${green}✔${white}] ${green}%s${reset}\n" "$@" ; }
error() { printf "${white}[${red}✖${white}] ${red}%s${reset}\n" "$@" ; }
warn() { printf "${white}[${tan}➜${white}] ${tan}%s${reset}\n" "$@" ; }
bold() { printf "${bold}%s${reset}\n" "$@" ; }
note() { printf "\n${underline}${bold}${blue}Note:${reset} ${blue}%s${reset}\n" "$@" ; }

#-----------------------------------------------------------------------------------------                   
# Functions
#-----------------------------------------------------------------------------------------                   
function usage {
    info "Syntax: build-locally.sh [OPTIONS]"
    cat << EOF
Options are:
-h | --help : Display this help text
--module The name of the module to start building from
--chain true/false/yes/no/y/n
EOF
}

function check_chain_is_true_or_false() {
    chain_input=$1

    if [[ "$chain_input" == "yes" ]] || [[ "$chain_input" == "y" ]] || [[ "$chain_input" == "true" ]]; then 
        chain="true"
    else
        if [[ "$chain_input" == "no" ]] || [[ "$chain_input" == "n" ]] || [[ "$chain_input" == "false" ]]; then 
            chain="false"
        else 
            error "--chain parameter value '$chain_input' should be yes,y,true,no,n or false."
            exit 1
        fi
    fi
    success "--chain option is value. $chain"
}

module_names=(\
    "wrapping" \
    "buildutils" \
    "gradle" \
    "maven" \
    "framework" \
    "extensions" \
    "managers" \
    "obr" \
    "cli" \
)

function check_module_name_is_supported() {
    module_input=$1
    is_valid="false"
    # Loop through all the keys of our command
    if [[ " ${module_names[*]} " =~ " ${module_input} " ]]; then
        is_valid="true"
    fi

    if [[ "$is_valid" != "true" ]]; then 
        msg="'$module_input' is an invalid module name. Valid module names are: [ ${module_names[*]} ]"
        error "$msg"
        exit 1
    fi

    success "--module '$module_input' is a valid module name."
}

#-----------------------------------------------------------------------------------------                   
# Process parameters
#-----------------------------------------------------------------------------------------
module_input="buildutils"
chain_input="true"
while [ "$1" != "" ]; do
    case $1 in
        -h | --help )   usage
                        exit
                        ;;

        --module )      module_input="$2"
                        shift
                        ;;

        --chain )       chain_input="$2"
                        shift
                        ;;

        * )             error "Unexpected argument $1"
                        usage
                        exit 1
    esac
    shift
done

check_chain_is_true_or_false $chain_input
# This gives us $chain holding "true" or "false"

check_module_name_is_supported $module_input


      
#-----------------------------------------------------------------------------------------                   
# Functions
#-----------------------------------------------------------------------------------------  

function clean_local_m2() {
    h2 "Cleaning up mave .m2 results"
    rm -fr ~/.m2/repository/dev/galasa/galasa*
    rm -fr ~/.m2/repository/dev/galasa/dev-galasa*
}

function build_module() {
    module=$1
    chain=$2
    h1 "Building... module:'$module' chain:'$chain'"

    # buildutils
    if [[ "$module" == "buildutils" ]]; then
        h2 "Building $module"
        cd ${PROJECT_DIR}/modules/$module
        ${PROJECT_DIR}/modules/$module/build-locally.sh
        rc=$? ;  if [[ "${rc}" != "0" ]]; then error "Failed to build module $module. rc=$rc" ; exit 1 ; fi
        success "Built module $module OK"
        if [[ "$chain" == "true" ]]; then
            module="wrapping"
        fi
    fi

    # wrapping
    if [[ "$module" == "wrapping" ]]; then
        h2 "Building $module"
        cd ${PROJECT_DIR}/modules/$module
        ${PROJECT_DIR}/modules/$module/build-locally.sh
        rc=$? ;  if [[ "${rc}" != "0" ]]; then error "Failed to build module $module. rc=$rc" ; exit 1 ; fi
        success "Built module $module OK"
        if [[ "$chain" == "true" ]]; then 
            module="gradle"
        fi
    fi

    # gradle
    if [[ "$module" == "gradle" ]]; then
        h2 "Building $module"
        cd ${PROJECT_DIR}/modules/$module
        ${PROJECT_DIR}/modules/$module/build-locally.sh
        rc=$? ;  if [[ "${rc}" != "0" ]]; then error "Failed to build module $module. rc=$rc" ; exit 1 ; fi
        success "Built module $module OK"
        if [[ "$chain" == "true" ]]; then 
            module="maven"
        fi
    fi

    # maven
    if [[ "$module" == "maven" ]]; then
        h2 "Building $module"
        cd ${PROJECT_DIR}/modules/$module
        ${PROJECT_DIR}/modules/$module/build-locally.sh
        rc=$? ;  if [[ "${rc}" != "0" ]]; then error "Failed to build module $module. rc=$rc" ; exit 1 ; fi
        success "Built module $module OK"
        if [[ "$chain" == "true" ]]; then 
            module="framework"
        fi
    fi

    # framework
    if [[ "$module" == "framework" ]]; then
        h2 "Building $module"
        cd ${PROJECT_DIR}/modules/$module
        ${PROJECT_DIR}/modules/$module/build-locally.sh --clean
        rc=$? ;  if [[ "${rc}" != "0" ]]; then error "Failed to build module $module. rc=$rc" ; exit 1 ; fi
        success "Built module $module OK"
        if [[ "$chain" == "true" ]]; then 
            module="extensions"
        fi
    fi

    # extensions
    if [[ "$module" == "extensions" ]]; then
        h2 "Building $module"
        cd ${PROJECT_DIR}/modules/$module
        ${PROJECT_DIR}/modules/$module/build-locally.sh --clean
        rc=$? ;  if [[ "${rc}" != "0" ]]; then error "Failed to build module $module. rc=$rc" ; exit 1 ; fi
        success "Built module $module OK"
        if [[ "$chain" == "true" ]]; then 
            module="managers"
        fi
    fi

    # managers
    if [[ "$module" == "managers" ]]; then
        h2 "Building $module"
        cd ${PROJECT_DIR}/modules/$module
        ${PROJECT_DIR}/modules/$module/build-locally.sh --clean
        rc=$? ;  if [[ "${rc}" != "0" ]]; then error "Failed to build module $module. rc=$rc" ; exit 1 ; fi
        success "Built module $module OK"
        if [[ "$chain" == "true" ]]; then 
            module="obr"
        fi
    fi

    # obr
    if [[ "$module" == "obr" ]]; then
        h2 "Building $module"
        cd ${PROJECT_DIR}/modules/$module
        ${PROJECT_DIR}/modules/$module/build-locally.sh
        rc=$? ;  if [[ "${rc}" != "0" ]]; then error "Failed to build module $module. rc=$rc" ; exit 1 ; fi
        success "Built module $module OK"
        if [[ "$chain" == "true" ]]; then 
            module="cli"
        fi
    fi

    # cli
    if [[ "$module" == "cli" ]]; then
        cd ${PROJECT_DIR}/modules/$module
        ${PROJECT_DIR}/modules/$module/build-locally.sh --clean
        rc=$? ;  if [[ "${rc}" != "0" ]]; then error "Failed to build module $module. rc=$rc" ; exit 1 ; fi
        success "Built module $module OK"
    fi

}

clean_local_m2
build_module $module_input $chain
