package de.paktosan;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @goal run
 * @requiresOnline true
 */
public class RunServerMojo extends AbstractMojo {
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    MavenProject project;
    /**
     * @parameter name="version"
     * @required
     */
    private String version;
    /**
     * @parameter default-value="${basedir}"
     */
    private File path;
    /**
     * @parameter default-value="-Xmx1g"
     */
    private String args;
    /**
     * @parameter default-value=" "
     */
    private String parameters;

    public void execute() throws MojoExecutionException, MojoFailureException {
        ProcessBuilder pb;
        Process p;
        System.out.println("Path is: "+path.toString());
        if (Files.notExists(Paths.get(path+"/server"))) {
            try {
                Files.createDirectory(Paths.get(path+"/server"));
            } catch (IOException e) {
                throw new MojoExecutionException("Could not create directory", e);
            }
        }
        if (Files.notExists(Paths.get(path+"/server/" + version + ".jar"))) {
            if (Files.notExists(Paths.get(path+"/servers"))) {
                try {
                    Files.createDirectory(Paths.get(path+"/servers"));
                } catch (IOException e) {
                    throw new MojoExecutionException("Could not create directory", e);
                }
            }
            if (Files.notExists(Paths.get(path+"/servers/BuildTools.jar"))) {
                getLog().info("Downloading Spigot BuildTools...");
                try {
                    FileUtils.copyURLToFile(new URL("https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar"),
                            new File(path+"/servers/BuildTools.jar"));
                } catch (IOException e) {
                    throw new MojoExecutionException("Oops", e);
                }
            }
            String[] strings = version.split("-");
            getLog().info("Building Spigot and CraftBukkit...");
            try {
                pb = new ProcessBuilder("java", "-jar", "BuildTools.jar", "-rev", strings[1]);
            } catch (ArrayIndexOutOfBoundsException e) {
                pb = new ProcessBuilder("java", "-jar", "BuildTools.jar");
            }
            pb.directory(new File(path.toString() + "/servers"));
            pb.inheritIO();
            try {
                p = pb.start();
            } catch (IOException e) {
                throw new MojoExecutionException("Oops", e);
            }
            while (p.isAlive()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new MojoExecutionException("Mum, i can´t sleep...", e);
                }
            }
            if (p.exitValue() != 0) {
                throw new MojoFailureException("Other process derped!");
            }
            getLog().info("Copying everything to server...");
            try {
                Files.copy(Paths.get(path.toString()+"/servers/" + version + ".jar"), Paths.get(path.toString()
                        +"/server/" +
                        version + ".jar"));
            } catch (IOException e) {
                throw new MojoExecutionException("Couldn´t copy the server!", e);
            }
        }
        if (Files.notExists(Paths.get(path.toString()+"/server/plugins"))) {
            try {
                Files.createDirectory(Paths.get(path.toString()+"/server/plugins"));
            } catch (IOException e) {
                throw new MojoExecutionException("Couldn´t create plugin dir" + e);
            }
        }
        if (Files.exists(Paths.get(path.toString()+"/server/plugins/"+project.getArtifact().getArtifactId()+".jar"))){
            getLog().info("Deleting old plugin version...");
            try {
                Files.delete(Paths.get(path.toString()+"/server/plugins/"+project.getArtifact().getArtifactId()+".jar"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Files.copy(Paths.get(String.valueOf(project.getArtifact().getFile())),
                    Paths.get(path.toString()+"/server/plugins/" + project.getArtifact().getArtifactId() + ".jar"));
        } catch (IOException e) {
            throw new MojoExecutionException("Couldn´t copy plugin!" + e);
        }
        getLog().info("Starting server...");
        pb = new ProcessBuilder("java",args, "-jar", version + ".jar", parameters);
        pb.directory(new File(path.toString() + "/server"));
        pb.inheritIO();
        try {
            p = pb.start();
        } catch (IOException e) {
            throw new MojoExecutionException("Oops", e);
        }
        while (p.isAlive()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new MojoExecutionException("Mum, i can´t sleep...", e);
            }
        }
        if (p.exitValue() != 0) {
            throw new MojoFailureException("Other process derped!");
        }
        getLog().info("We are done!");
    }
}
