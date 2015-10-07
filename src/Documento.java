
import java.io.*;
import java.util.Date;
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
        } catch(Exception e) {
            System.out.println("El archivo dado no existe.");
        }

        this.camino = camino;
        nombre = archivo.getName();

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
