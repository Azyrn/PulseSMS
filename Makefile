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
GRADLE_OPTS := --configuration-cache
GRADLE := ./gradlew $(GRADLE_OPTS)
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
	@printf "$(BOLD)Configuration Cache :$(NC) $(GREEN)ACTIVE$(NC) ($(GRADLE_OPTS))\n"
	@printf "\n"
	@printf "$(BOLD)Utilisation :$(NC) make [target]\n"
	@printf "\n"
	@printf "$(BOLD)Targets disponibles :$(NC)\n"
	@printf "  $(GREEN)all$(NC)          - Compile, installe et lance (CONSERVE le cache)\n"
	@printf "  $(GREEN)rebuild$(NC)      - Nettoie, compile, installe et lance (DETRUIT le cache)\n"
	@printf "  $(GREEN)clean$(NC)        - Nettoie le build (CONSERVE le cache)\n"
	@printf "  $(GREEN)clean-all$(NC)    - Nettoie build + Configuration Cache\n"
	@printf "  $(GREEN)clean-cache$(NC)  - Supprime UNIQUEMENT le Configuration Cache\n"
	@printf "  $(GREEN)build$(NC)        - Compile l'APK debug (avec cache)\n"
	@printf "  $(GREEN)build-release$(NC) - Compile l'APK release (avec cache)\n"
	@printf "  $(GREEN)install$(NC)      - Installe l'APK (avec -r -d pour downgrade/debug)\n"
	@printf "  $(GREEN)reinstall$(NC)    - Desinstalle puis installe (propre)\n"
	@printf "  $(GREEN)run$(NC)          - Lance l'application\n"
	@printf "  $(GREEN)stop$(NC)         - Force l'arret de l'application\n"
	@printf "  $(GREEN)restart$(NC)      - Force l'arret et relance\n"
	@printf "  $(GREEN)uninstall$(NC)    - Desinstalle l'application\n"
	@printf "  $(GREEN)logs$(NC)         - Logs filtres par PID\n"
	@printf "  $(GREEN)logs-grep$(NC)    - Logs filtres par nom de package\n"
	@printf "  $(GREEN)logs-raw$(NC)     - Tous les logs sans filtre\n"
	@printf "  $(GREEN)logs-clear$(NC)   - Efface le buffer puis affiche les logs\n"
	@printf "  $(GREEN)logs-error$(NC)   - Uniquement erreurs et fatals\n"
	@printf "  $(GREEN)devices$(NC)      - Liste les appareils connectes\n"
	@printf "  $(GREEN)check-adb$(NC)    - Verifie qu'un appareil est connecte\n"
	@printf "  $(GREEN)help$(NC)         - Affiche cette aide\n"
	@printf "\n"
	@printf "$(BOLD)Workflow recommande :$(NC)\n"
	@printf "  $(CYAN)make all$(NC)           # Developpement rapide (cache chaud)\n"
	@printf "  $(CYAN)make reinstall$(NC)     # Si install echoue (conflit de signature)\n"
	@printf "  $(CYAN)make rebuild$(NC)       # Si probleme de build (cache froid)\n"
	@printf "  $(CYAN)make clean-cache$(NC)   # Si le cache est corrompu\n"
	@printf "$(BLUE)=============================================$(NC)\n"

# --- Verification ADB ---
.PHONY: check-adb
check-adb:
	@devices=$$($(ADB) devices 2>/dev/null | tr -d '\r' | sed '1d' | awk '$$2 == "device" {count++; print $$1}'); \
	if [ -z "$$devices" ]; then \
		printf "$(YELLOW)ADB : aucun appareil detecte, tentative de redemarrage du serveur...$(NC)\n"; \
		$(ADB) kill-server 2>/dev/null; \
		sleep 1; \
		$(ADB) start-server 2>/dev/null; \
		sleep 2; \
		devices=$$($(ADB) devices 2>/dev/null | tr -d '\r' | sed '1d' | awk '$$2 == "device" {count++; print $$1}'); \
		if [ -z "$$devices" ]; then \
			printf "$(RED)Erreur : Aucun appareil Android connecte ou debogage USB non active.$(NC)\n"; \
			printf "   -> Branchez un appareil et activez le $(BOLD)debogage USB$(NC).\n"; \
			printf "   -> Verifiez le cable et les permissions USB.\n"; \
			exit 1; \
		fi; \
		printf "$(GREEN)Appareil detecte apres redemarrage ADB :$(NC)\n"; \
		printf "$$devices\n"; \
	fi

# --- Nettoyage build (Configuration Cache conserve) ---
.PHONY: clean
clean:
	@printf "$(YELLOW)Nettoyage du build (cache conserve)...$(NC)\n"
	$(GRADLE) clean

# --- Nettoyage complet (build + cache) ---
.PHONY: clean-all
clean-all:
	@printf "$(YELLOW)Nettoyage complet (build + Configuration Cache)...$(NC)\n"
	$(GRADLE) clean
	@rm -rf .gradle/configuration-cache

# --- Suppression uniquement du Configuration Cache ---
.PHONY: clean-cache
clean-cache:
	@printf "$(YELLOW)Suppression du Configuration Cache...$(NC)\n"
	@rm -rf .gradle/configuration-cache
	@printf "$(GREEN)Configuration Cache supprime. Prochain build sera complet.$(NC)\n"

# --- Compilation debug ---
.PHONY: build
build:
	@printf "$(YELLOW)Compilation debug (Configuration Cache active)...$(NC)\n"
	$(GRADLE) assembleDebug

# --- Compilation release ---
.PHONY: build-release
build-release:
	@printf "$(YELLOW)Compilation release (Configuration Cache active)...$(NC)\n"
	$(GRADLE) assembleRelease

# --- Installation (avec gestion des erreurs) ---
.PHONY: install
install: check-adb build
	@printf "$(YELLOW)Installation de l'APK (debug)...$(NC)\n"
	@if $(ADB) shell pm list packages | grep -q $(PACKAGE_NAME); then \
		printf "$(CYAN)App deja installee, tentative de mise a jour...$(NC)\n"; \
	fi
	@$(ADB) install -r -t -d $(APK_DEBUG_PATH) || { \
		printf "$(RED)Echec de l'installation.$(NC)\n"; \
		printf "   -> Essayez : $(BOLD)make reinstall$(NC) (desinstalle puis reinstalle)\n"; \
		printf "   -> Ou verifiez : $(BOLD)adb logcat$(NC) pour l'erreur detaillee\n"; \
		exit 1; \
	}

# --- Reinstallation propre (desinstalle + installe) ---
.PHONY: reinstall
reinstall: check-adb uninstall install

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
	-$(ADB) uninstall $(PACKAGE_NAME) 2>/dev/null || true

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
all: install run            # Rapide : conserve le Configuration Cache
rebuild: clean-all install run  # Complet : reset tout
