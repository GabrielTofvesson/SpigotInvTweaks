#!/bin/bash

MIN_JAVA_VERSION=15
[ "$JAVA_PATH" == "" ] && JAVA_PATH=$(which java)
[ "$PYTHON_PATH" == "" ] && PYTHON_PATH=$(which python3)

SCRIPT_RUN_NAME="start.sh"
SCRIPT_DEBUG_NAME="start_debug.sh"

MC_VERSION="1.16"
ENV_DIR="$(pwd)/test_server"
PAPER_JAR_FILE="paper.jar"
PLUGIN_PATH=""
UPDATE_PAPER=true
AUTORUN=""
EULA=false

print_help() {
  echo -e "Paper test server deployment script\nArguments:"
  echo "  -v [VERSION]    -   Specify minecraft version to download"
  echo "  -p [PATH]       -   Path to plugin to debug"
  echo "  -d [PATH]       -   Path to directory where server will be deployed"
  echo "  -j [PATH]       -   Java executable path"
  echo "  --python [PATH] -   Python3 executable path"
  echo "  --no-update     -   Don't download updates for Paper if jar file already exists"
  echo "  -n              -   Same as --no-update"
  echo "  -s [type]       -   Autorun server after deployment. Type is \"debug\" to start with remote debugging, else \"run\""
  echo "  -e              -   Accepts the EULA for the deployed server (user must accept it in a provided prompt)"
  echo "  -h              -   Show this help prompt"
}

print_error() {
  echo "[ERROR] $1" >&2
  return 1
}

print_info() {
  if [ $# -eq 2 ]
  then
    echo -e "$1" "[TEST_ENV] $2"
  else
    echo -e "[TEST_ENV] $1"
  fi
  return 1
}


SKIP=false
CUR=""
TARGET=""
for arg in "$@"
do
  if $SKIP
  then
    eval "$TARGET"="$arg"
    SKIP=false
    continue
  fi

  CUR="$arg"

  case "$arg" in
    "-h")
      print_help
      exit
      ;;

    "-v")
      TARGET="MC_VERSION"
      SKIP=true
      ;;

    "-p")
      TARGET="PLUGIN_PATH"
      SKIP=true
      ;;

    "-d")
      TARGET="ENV_DIR"
      SKIP=true
      ;;

    "-j")
      TARGET="JAVA_PATH"
      SKIP=true
      ;;

    "--python")
      TARGET="PYTHON_PATH"
      SKIP=true
      ;;

    "-n" | "--no-update")
      UPDATE_PAPER=false
      ;;

    "-s")
      TARGET="AUTORUN"
      SKIP=true
      ;;

    "-e")
      EULA=true
      ;;
  esac
done

if $SKIP
then
  if [ "$CUR" == "-s" ]
  then
    AUTORUN="run"
  else
    print_error "Malformed parameter $CUR"
    exit
  fi
fi

case "$AUTORUN" in
  "run")
    AUTORUN="$SCRIPT_RUN_NAME"
    ;;
  "debug")
    AUTORUN="$SCRIPT_DEBUG_NAME"
    ;;
  *)
    print_error "Unknown autorun argument \"$AUTORUN\""
    exit
    ;;
esac

check_java_version() {
  version=$("$JAVA_PATH" -version 2>&1 | head -n 1 | sed "s/java version \"\\([0-9]*\\)\\..*/\\1/g")
  if [ "$version" -lt "$MIN_JAVA_VERSION" ]
  then
    print_error "Java version is too low! Minimum is $MIN_JAVA_VERSION (found $version)"
    return 1
  fi
  print_info "Found Java version $version"
  return 0
}

ensure_python3() {
  if [ "$PYTHON_PATH" == "" ]
  then
    print_error "Could not locate Python3 which is required for automatic server deployment: please install it"
    return 1
  fi
  print_info "Found Python3 binary"
  return 0
}

get_paper() {

  print_info "Polling all versions of paper for MC $MC_VERSION"

  link=$(curl -s "https://papermc.io/api/v2/projects/paper/version_group/$MC_VERSION/builds" | python3 -c "import sys, json; versions=json.load(sys.stdin); print('https://papermc.io/api/v2/projects/paper/versions/' + str(versions['builds'][-1]['version']) + '/builds/' + str(versions['builds'][-1]['build']) + '/downloads/' + str(versions['builds'][-1]['downloads']['application']['name']))")

  if [ -f "new_$PAPER_JAR_FILE" ]
  then
    rm -f "new_$PAPER_JAR_FILE"
  fi

  print_info "Downloading from pulled link: $link"

  result=$(curl -o "new_$PAPER_JAR_FILE" -s -w "%{http_code}" "$link")

  if [ "$result" -eq 200 ] && [ -f "new_$PAPER_JAR_FILE" ]
  then
    rm -f "$PAPER_JAR_FILE"
    mv "new_$PAPER_JAR_FILE" "$PAPER_JAR_FILE"
    print_info "Downloaded latest version of paper"
    return 1
  else
    print_info "Could not download new version of paper"
    return 0
  fi
}

check_java_version || exit
ensure_python3 || exit


if ! [ -d "$ENV_DIR/plugins" ]
then
  mkdir -p "$ENV_DIR/plugins" || (print_error "Could not create server directory at $ENV_DIR" && exit)
fi

if [ "$PLUGIN_PATH" != "" ]
then
  cp "$PLUGIN_PATH" "$ENV_DIR/plugins" || (print_error "Could not copy targeted plugin to test environment" && exit)
fi

cd "$ENV_DIR" || (print_error "Could not enter directory $ENV_DIR" && exit)
(! [ -f "$PAPER_JAR_FILE" ] || $UPDATE_PAPER) && get_paper

[ -f "$PAPER_JAR_FILE" ] || exit

if ! [ -f "$SCRIPT_RUN_NAME" ]
then
  echo -e "#!/bin/bash\njava -jar \"$PAPER_JAR_FILE\" nogui" > "$SCRIPT_RUN_NAME"
fi

if ! [ -f "$SCRIPT_DEBUG_NAME" ]
then
  echo -e "#!/bin/bash\njava -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar \"$PAPER_JAR_FILE\" nogui" > "$SCRIPT_DEBUG_NAME"
fi

print_info "Test environment successfully prepared!"

if $EULA
then
  print_info -n "Do you accept the terms of the Minecraft End User License Agreement? (https://account.mojang.com/documents/minecraft_eula) [y/N]: "
  read -r ACCEPTS

  case "$ACCEPTS" in
    "y"* | "Y"*)
      echo "eula=true" > eula.txt
      ;;
    *)
      print_info "EULA was not accepted"
      ;;
  esac
fi

if [ "$AUTORUN" != "" ]
then
  print_info "Autorun was specified. Starting server..."
  ./$AUTORUN
fi