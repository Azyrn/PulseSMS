# =============================================
# Makefile pour PulseSMS (Android)
# =============================================

# --- Détection des couleurs ---
ifdef NO_COLOR
    GREEN := YELLOW := RED := BLUE := CYAN := BOLD := NC :=
else
    GREEN := \033[0;32m
    YELLOW := \033[1;33m
    RED := \033[0;31m
    BLUE := \033[0;34m
    CYAN := \033[0;36m
    BOLD := \033[1m
    NC := \033[0m
endif

# --- Variables ---
GRADLE := _JAVA_OPTIONS="-Djava.net.preferIPv6Addresses=true" ./gradlew
ADB := adb

PACKAGE_NAME := com.skeler.pulse
MAIN_ACTIVITY := com.skeler.pulse.MainActivity
APK_DEBUG_PATH := app/build/outputs/apk/debug/app-universal-debug.apk

# --- Cible par défaut ---
default: help

# --- Aide ---
.PHONY: help
help:
	@printf "$(BLUE)=============================================$(NC)\n"
	@printf "$(BOLD)$(BLUE)Makefile pour PulseSMS$(NC)\n"
	@printf "$(BLUE)=============================================$(NC)\n"
	@printf "\n"
	@printf "$(BOLD)Utilisation :$(NC) make [target]\n"
	@printf "\n"
	@printf "$(BOLD)Targets disponibles :$(NC)\n"
	@printf "  $(GREEN)all$(NC)          - Nettoie, compile, installe et lance l'app\n"
	@printf "  $(GREEN)clean$(NC)        - Nettoie le projet\n"
	@printf "  $(GREEN)build$(NC)        - Compile l'APK en mode debug\n"
	@printf "  $(GREEN)build-release$(NC) - Compile l'APK en mode release\n"
	@printf "  $(GREEN)install$(NC)      - Compile et installe l'APK debug\n"
	@printf "  $(GREEN)run$(NC)          - Lance l'application\n"
	@printf "  $(GREEN)stop$(NC)         - Force l'arret de l'application\n"
	@printf "  $(GREEN)restart$(NC)      - Force l'arret et relance l'application\n"
	@printf "  $(GREEN)uninstall$(NC)    - Desinstalle l'application\n"
	@printf "  $(GREEN)rebuild$(NC)      - Nettoie, compile, installe et lance\n"
	@printf "  $(GREEN)logs$(NC)         - Logs filtrés par PID (app doit tourner)\n"
	@printf "  $(GREEN)logs-grep$(NC)    - Logs filtrés par nom de package\n"
	@printf "  $(GREEN)logs-raw$(NC)     - Tous les logs sans filtre\n"
	@printf "  $(GREEN)logs-clear$(NC)   - Efface le buffer puis affiche les logs\n"
	@printf "  $(GREEN)logs-error$(NC)   - Uniquement erreurs et fatals\n"
	@printf "  $(GREEN)devices$(NC)      - Liste les appareils connectés\n"
	@printf "  $(GREEN)check-adb$(NC)    - Verifie qu'un appareil est connecté\n"
	@printf "  $(GREEN)help$(NC)         - Affiche cette aide\n"
	@printf "\n"
	@printf "$(BOLD)Exemples :$(NC)\n"
	@printf "  make              # Affiche cette aide\n"
	@printf "  make all          # Nettoie, compile, installe et lance\n"
	@printf "  make install      # Compile et installe (sans lancer)\n"
	@printf "  make logs         # Logs en temps reel (PID)\n"
	@printf "  make logs-clear   # Logs propres (buffer vide)\n"
	@printf "$(BLUE)=============================================$(NC)\n"

# --- Verification ADB ---
.PHONY: check-adb
check-adb:
	@if ! $(ADB) devices | grep -q "device$$"; then \
		printf "$(RED)Erreur : Aucun appareil Android connecte ou debogage USB non active.$(NC)\n"; \
		printf "   -> Branchez un appareil et activez le $(BOLD)debogage USB$(NC).\n"; \
		exit 1; \
	fi

# --- Nettoyage ---
.PHONY: clean
clean:
	@printf "$(YELLOW)Nettoyage du projet...$(NC)\n"
	$(GRADLE) clean

# --- Compilation (pas besoin d'ADB) ---
.PHONY: build
build:
	@printf "$(YELLOW)Compilation de l'APK (debug)...$(NC)\n"
	$(GRADLE) assembleDebug

.PHONY: build-release
build-release:
	@printf "$(YELLOW)Compilation de l'APK (release)...$(NC)\n"
	$(GRADLE) assembleRelease

# --- Installation (depend de build pour etre sur que l'APK existe) ---
.PHONY: install
install: check-adb build
	@printf "$(YELLOW)Installation de l'APK (debug)...$(NC)\n"
	$(ADB) install -r -t $(APK_DEBUG_PATH)

# --- Execution ---
.PHONY: run
run: check-adb
	@printf "$(YELLOW)Lancement de l'application...$(NC)\n"
	$(ADB) shell am start -n $(PACKAGE_NAME)/$(MAIN_ACTIVITY)

# --- Arret force ---
.PHONY: stop
stop: check-adb
	@printf "$(YELLOW)Arret force de l'application...$(NC)\n"
	$(ADB) shell am force-stop $(PACKAGE_NAME)

# --- Redemarrage ---
.PHONY: restart
restart: stop run

# --- Desinstallation ---
.PHONY: uninstall
uninstall: check-adb
	@printf "$(YELLOW)Desinstallation de l'application...$(NC)\n"
	$(ADB) uninstall $(PACKAGE_NAME)

# --- Logs via PID ---
.PHONY: logs
logs: check-adb
	@printf "$(YELLOW)Affichage des logs via PID (Ctrl+C pour arreter)...$(NC)\n"
	@PID=$$($(ADB) shell pidof $(PACKAGE_NAME)); \
	if [ -z "$$PID" ]; then \
		printf "$(RED)Erreur : $(PACKAGE_NAME) n'est pas en cours d'execution.$(NC)\n"; \
		printf "   -> Lancez d'abord : $(BOLD)make run$(NC)\n"; \
		exit 1; \
	fi; \
	printf "$(CYAN)PID detecte : $$PID$(NC)\n"; \
	$(ADB) logcat --pid=$$PID

# --- Logs via grep ---
.PHONY: logs-grep
logs-grep: check-adb
	@printf "$(YELLOW)Logs filtres par '$(PACKAGE_NAME)' (Ctrl+C pour arreter)...$(NC)\n"
	$(ADB) logcat | grep -E "$(PACKAGE_NAME)|AndroidRuntime|System\.err"

# --- Logs bruts ---
.PHONY: logs-raw
logs-raw: check-adb
	@printf "$(YELLOW)Tous les logs (Ctrl+C pour arreter)...$(NC)\n"
	$(ADB) logcat

# --- Logs avec effacement du buffer ---
.PHONY: logs-clear
logs-clear: check-adb
	@printf "$(YELLOW)Effacement du buffer...$(NC)\n"
	-@$(ADB) logcat -c 2>/dev/null || true
	@printf "$(GREEN)Buffer efface. Lancement des logs...$(NC)\n"
	@PID=$$($(ADB) shell pidof $(PACKAGE_NAME)); \
	if [ -z "$$PID" ]; then \
		printf "$(RED)App non lancee, fallback sur grep...$(NC)\n"; \
		$(ADB) logcat | grep -E "$(PACKAGE_NAME)|AndroidRuntime|System\.err"; \
	else \
		printf "$(CYAN)PID : $$PID$(NC)\n"; \
		$(ADB) logcat --pid=$$PID; \
	fi

# --- Logs erreurs uniquement ---
.PHONY: logs-error
logs-error: check-adb
	@printf "$(YELLOW)Erreurs et fatals uniquement (Ctrl+C pour arreter)...$(NC)\n"
	@PID=$$($(ADB) shell pidof $(PACKAGE_NAME)); \
	if [ -n "$$PID" ]; then \
		$(ADB) logcat *:E --pid=$$PID; \
	else \
		$(ADB) logcat *:E | grep -E "$(PACKAGE_NAME)|AndroidRuntime|System\.err"; \
	fi

# --- Appareils connectes ---
.PHONY: devices
devices:
	@printf "$(YELLOW)Appareils connectes :$(NC)\n"
	$(ADB) devices

# --- Cibles combinees ---
.PHONY: all rebuild
all: clean install run
rebuild: clean install run
