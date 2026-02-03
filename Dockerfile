# Dockerfile pour build Ant + JavaFX
FROM eclipse-temurin:17-jdk

# Installer Ant, JavaFX et utilitaires de base
RUN apt-get update && \
    apt-get install -y ant openjfx wget unzip && \
    rm -rf /var/lib/apt/lists/*

# Définir la variable d'environnement JavaFX
ENV JAVA_FX_HOME=/usr/share/openjfx

# Définir le dossier de travail
WORKDIR /root

# Copier le projet dans l'image
ADD . ./

# Commande par défaut : juste build (pas les tests)
ENTRYPOINT ["ant"]
CMD []