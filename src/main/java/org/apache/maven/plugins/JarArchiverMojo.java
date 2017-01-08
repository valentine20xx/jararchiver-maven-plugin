package org.apache.maven.plugins;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

@Mojo(
        name = "createJar",
        defaultPhase = LifecyclePhase.PACKAGE,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class JarArchiverMojo extends AbstractMojo {
    @Parameter(property = "mainClass", required = true, readonly = true)
    private String mainClass;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    private List<String> fileList = new ArrayList<>();

    private String EXTENSION = "jar";

    public void execute() throws MojoExecutionException, MojoFailureException {
        Build build = project.getBuild();

        getLog().info("Source directory : " + getSourceDirectory());
        getLog().info("Classes Directory : " + getClassesDirectory());
        getLog().info("Target Directory : " + getTargetDirectory());
        getLog().info("mainClass : " + mainClass);

        prepareFileList(new File(getClassesDirectory()));
        createJarFile();
    }


    private void createJarFile() throws MojoExecutionException {
        String path = getTargetDirectory() + File.separator + getBuildFileName() + "." + EXTENSION;

        try (OutputStream outputStream = new FileOutputStream(path)) {
            try (JarOutputStream jarOutputStream = new JarOutputStream(outputStream, getManifest())) {
                for (String file : fileList) {
                    getLog().info("File to jar : " + file);

                    JarEntry jarEntry = new JarEntry(file.replace("\\", "/"));
                    jarOutputStream.putNextEntry(jarEntry);
                    jarOutputStream.write(inputStreamToByteArray(new FileInputStream(getClassesDirectory() + File.separator + file)));
                    jarOutputStream.closeEntry();
                }
            }
        } catch (FileNotFoundException e) {
            String message = path + " not found !";
            getLog().error(message);
            throw new MojoExecutionException(message);
        } catch (IOException e) {
            throw new MojoExecutionException("System exception");
        }
    }

    /**
     * Подготовка списка файлов для добвления в jar
     *
     * @param root Корневая папка, откуда производится поиск
     */
    private void prepareFileList(File root) {
        // add file only
        if (root.isFile()) {
            fileList.add(generateZipEntry(root.toString()));
        }

        if (root.isDirectory()) {
            String[] subNote = root.list();
            for (String filename : subNote) {
                prepareFileList(new File(root, filename));
            }
        }
    }

    private byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (true) {
            int r = inputStream.read(buf);
            if (r == -1) {
                break;
            }
            out.write(buf, 0, r);
        }

        return out.toByteArray();
    }

    private Manifest getManifest() {
        Manifest manifest = new Manifest();
        Attributes global = manifest.getMainAttributes();
        global.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        global.put(new Attributes.Name("Created-By"), "Niko");
        global.put(Attributes.Name.MAIN_CLASS, mainClass);

        return manifest;
    }

    /**
     * Извлечение отностительного пути
     *
     * @param filePath Полный путь файла
     * @return Относительный путь файла от папки со скомпилированными файлами
     */
    private String generateZipEntry(String filePath) {
        return filePath.substring(getClassesDirectory().length() + 1, filePath.length());
    }

    private String getBuildFileName() {
        return getBuild().getFinalName();
    }

    private Build getBuild() {
        return project.getBuild();
    }

    private String getClassesDirectory() {
        return getBuild().getOutputDirectory();
    }

    private String getTargetDirectory() {
        return getBuild().getDirectory();
    }

    private String getSourceDirectory() {
        return getBuild().getSourceDirectory();
    }
}