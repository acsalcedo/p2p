# p2p
Proyecto 1 para Sistemas de Operación 3 - Sept-Dic 2015

### Cómo correr el proyecto en Linux

1. Ubicarse en el directorio src/
2. Compilar la clase Nodo con el siguiente comando:

		javac -classpath ../lib/jade.jar:. Nodo.java

3. Correr la plataforma de JADE:

		java -classpath ../lib/jade.jar jade.Boot -gui -host localhost

4. Correr el distribuidor de los archivos:

		java -classpath ../lib/jade.jar:. jade.Boot -container distribuidor:Nodo

5. Correr el solicitante de archivos para un archivo en específico:

		java -classpath ../lib/jade.jar:. jade.Boot -container 'solicitante:Nodo(ejemplo1.txt)'

6. Correr el solicitante de archivos para un tipo de archivo:

		java -classpath ../lib/jade.jar:. jade.Boot -container 'solicitante:Nodo(txt)'

7. Correr el solicitante de CPU:

		java -classpath ../lib/jade.jar:. jade.Boot -container 'solicitante:Nodo(script.sh,main.java)'
