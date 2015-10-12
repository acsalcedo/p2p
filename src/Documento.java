import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.File;

/**
 *  Modulo: Documento
 *  Descripcion: Clase para traslado de archivos en red.
*/
public class Documento implements Serializable {

    private byte[] contenido;
    private String camino;
    private String nombre;
    private int descargas = 0;

    public Documento(String camino) {

        File archivo = new File(camino);
        try {
            byte[] temp = new byte[(int) archivo.length()];
            BufferedInputStream entrada = new
                BufferedInputStream(new FileInputStream(archivo));

            entrada.read(temp,0,temp.length);
            entrada.close();
            contenido = temp;
            this.camino = camino;
            nombre = archivo.getName();
        } catch(Exception e) {
            System.out.println("El archivo dado no existe.");
        }
    }

    public Documento(String nombreArchivo, byte[] contenido) {
        this.contenido = contenido;
        nombre = nombreArchivo;
        descargas = 0;
        camino = nombreArchivo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCamino() {
        return camino;
    }

    public byte[] getContenidoByte() {
        return contenido;
    }

    public int getDescargas() {
        return descargas;
    }

    public void incrDescargas() {
        descargas += 1;
    }

    public String toString() {
        try {
            if (contenido != null) {
           String cont = new String(contenido, "UTF-8");
           return cont;
            }
       } catch (UnsupportedEncodingException uee) {
            System.out.println("Documento: "+ uee.getMessage());
       }
        return null;
    }
}
