package src;
import src.Estructuras.*;

import java.io.*;
import java.util.*;

public class Main {

    static List<Reg_t> banco_registros = new ArrayList<Reg_t>(); //Tam 16
    static List<Integer> memoria_datos = new ArrayList<>(); //Tam 32
    static List<Instruccion> memoria_instrucciones = new ArrayList<Instruccion>(); //Tam 32

    static List<UF_t> UF = new ArrayList<UF_t>(); //Tam 3
    static Map<Integer, List <Estacion_reg_t>> ER = new HashMap<>(); //Tam [3][32]
    static List<ROB_t> ROB = new ArrayList<ROB_t>(); //Tam 32
    static Map<Integer, LinkedList<Integer>> p_er_cola = new HashMap<>(); //Creamos un mapa con 3 entradas para cada UF y cada una asociada a una linked list para la cola de espera simulando FIFO

    /* ------- Contadores ------- */
    static int inst_programa = 0; //Cantidad de instrucciones en el programa
    static int inst_rob = 0; //Instrucciones en ROB
    static int ciclo = 1;

    static int act_rd = -1; //Actualizar rd en commit
    static int act_rs = -1; //Actualizar rs en commit
    static int act_rt = -1; //Actualizar rt en commit

    static int fin_programa = -1;

    /* ------- Punteros ------- */
    static int p_rob_cabeza, p_rob_cola = 0; //Puntero a las posiciones de rob para introducir (cola) o retirar (cabeza)
    static int PC = 0; //Puntero a memoria de instrucciones, siguiente instrucción a IF
    public static void main(String[] args) throws FileNotFoundException {
            /* ------- Valores iniciales ------- */
            int cant_UF = 3; //Cantidad de Unidades Funcionales
            int cant_instr = 32; //Cantidad de instrucciones
            int cant_mem_datos = 32; //Cantidad de entradas en la memoria de datos
            int cant_banco_registros = 16; //Cantidad de entradas en el banco de registros

            /* ------- Inicializaciones ------- */
            Inicializar_Memoria_Instrucciones(memoria_instrucciones, cant_instr);
            Carga_programa(memoria_instrucciones, "src/src/fichero1.txt");
            Inicializar_ER(ER, cant_UF, cant_instr);
            Inicializar_ROB(ROB, memoria_instrucciones, cant_instr, inst_programa);
            Inicializar_Banco_Registros(banco_registros, cant_banco_registros);
            Inicializar_Memoria_Datos(memoria_datos, cant_mem_datos);
            Inicializar_UF(UF, cant_UF);

            /* ------- Programa ------- */
        while(fin_programa != 0){
            if (ciclo == 1) {
                System.out.println("Cauce de ejecución");
            }
            Commit();
            WB();
            EX();
            ISS();
            Mostrar_Por_Pantalla();
            ciclo++;
            if(ciclo == 40)
                break;
        }


    }

    public static void Carga_programa(List<Instruccion> memoria_instrucciones, String fichero){
        //Se modifican inst_prog y la memoria respecto a la cantidad de datos del fichero
        BufferedReader reader;
        fin_programa = 0;
        try{
            reader = new BufferedReader(new FileReader(fichero));
            String linea = reader.readLine();
            while(linea != null){
                Instruccion instruccion = conversionInstruccion(linea);
                memoria_instrucciones.set(inst_programa, instruccion);
                linea = reader.readLine();
                inst_programa ++;
                fin_programa ++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Instruccion conversionInstruccion(String cadena){
        String rd = "";
        String rs = "";
        String rt = "";
        String cod = "";
        int inm = 0;
        Instruccion instruccion;
        String[] listado = cadena.strip().split("--");
        cod = listado[0];
        switch (cod){
            case("add"), ("sub"), ("fadd"), ("fsub"): //add--rd--,--rs--,--rt //sub--rd--,--rs--,--rt
                rd = listado[1];
                rs = listado[3];
                rt = listado[5];
                inm = 0;
                break;
            case("ld"), ("sd"): //ld--rt--,--inm--(--rs--) //sd--rt--,--inm--(--rs--)
                rd = "0";
                rt = listado[1];
                inm = Integer.parseInt(listado[3]);
                rs = listado[5];
                break;
            case("mult"), ("fmult"): //mult--rd--,--rt--,--rs
                rd = listado[1];
                rs = listado[5];
                rt = listado[3];
                inm = 0;
                break;
        }instruccion = new Instruccion(cod, rd, rs, rt, inm);
        return instruccion;
    }

    public static void Inicializar_Memoria_Instrucciones(List<Instruccion> memoria_instrucciones, int cant){
        for(int i = 0; i < cant; i++){
            memoria_instrucciones.add(new Instruccion("", "", "", "", 0));
        }
    }
    public static void Inicializar_ER(Map<Integer, List <Estacion_reg_t>> ER, int cant_UF, int cant_instrucciones){
        for(int key = 0; key < cant_UF; key ++){
            List <Estacion_reg_t> listado = new ArrayList<>();
            ER.put(key, listado);
        }
    }

    public static void Inicializar_ROB(List<ROB_t> ROB, List<Instruccion> memoria_instrucciones, int cant_instr, int inst_programa){
        for(int i = 0; i < cant_instr; i++) {
            if (inst_programa < cant_instr) {
                Instruccion instruccion = memoria_instrucciones.get(i);
                ROB.add(new ROB_t(i, instruccion, 0, instruccion.rd, 0, -1, -1, -1));
            }else{
                ROB.add(new ROB_t(i, null, 0, "", 0, -1,-1, -1));
            }
        }
    }


    public static void Inicializar_Banco_Registros(List<Reg_t> banco_Reg, int cant){
        for(int i = 0; i < cant; i++){
            String nombre_reg = "R" + Integer.toString(i);
            banco_Reg.add(new Reg_t(nombre_reg,0, 1,0,-1));
        }
    }

    public static void Inicializar_Memoria_Datos(List<Integer> memoria_datos, int cant){
        for(int i = 0; i < cant; i++){
            memoria_datos.add(i);
        }
    } //Almaceno cargas y almacenamientos. Carga obtiene el contenido de dicha posición
                                                                                             //y almacenamiento actualiza la posición en memoria

    public static void Inicializar_UF(List<UF_t> UF, int cant){
        for(int i = 0; i < cant; i++){
            UF.add(new UF_t(0, 0, -1, "---", "---", "---", 0, 0, 0));
            p_er_cola.put(i, new LinkedList<>());
        }
    }
    public static int Encontrar_Entrada_ROB(List<ROB_t> ROB, Instruccion instruccion){
        for(int i = 0; i < ROB.size(); i++){
            if(ROB.get(i).instruccion == instruccion)
                return i;
        }
        return -1;
    }

    public static int Encontrar_Destino_ROB(List<ROB_t> ROB, String registro){ //Comprobar si mis operandos(no destino) están siendo modificados en instrucciones anteriores. Devuelve entrada ROB
        for(int i = 0; i < ROB.size(); i++){
            if(ROB.get(i).busy == 1){
                if(ROB.get(i).destino.compareTo(registro) == 0){
                    return i;
                }
            }
        }return -1;
    }

    public static int Encontrar_Registro(List<Reg_t> Reg, String registro){ //Devuelvo el índice para modificar directamente
        for (int i = 0; i < Reg.size(); i++) {
            if (Reg.get(i).contenido.compareTo(registro) == 0) {
                return i;
            }
        }

        return -1;
    }

    public static int Encontrar_Estacion(List<Estacion_reg_t> er, int TAG_ROB){
        for(int i = 0; i < er.size(); i++)
            if(er.get(i).TAG_ROB == TAG_ROB)
                return i;
        return -1;
    }

    public static int Encontrar_UF_con_linea_ROB(List<UF_t> uf, int tag_rob){
        for(int i = 0; i < uf.size(); i++){
            if (uf.get(i).TAG_ROB == tag_rob)
                return i;
        }return -1;
    }

    public static boolean Encontrar_Destinos_Previos(List<ROB_t> rob, int ult_rob, String reg){//Comprobar que dicho registro no se tiene que actualizar previamente
        for(int i = 0; i < ult_rob; i++){
            if(rob.get(i).destino.compareTo(reg) == 0)
                return true;
        }return false;
    }

    public static void Actualizar_Registro_Usado(String registro){
        int pos_reg = -1; //Se usará para reemplazar en ER con los nombres de los registros
        for(int i = 0; i < banco_registros.size(); i++){ //Actualizamos en banco de registros
            if(banco_registros.get(i).contenido.compareTo(registro) == 0) { //Actualiza registro
                pos_reg = banco_registros.get(i).TAG_ROB; //En que ROB ha sido actualizado
                banco_registros.get(i).TAG_ROB = -1;
                banco_registros.get(i).ok = 1;
                banco_registros.get(i).clk_tick_ok = ciclo;
                break;
            }
        }int TAG_ROB = Encontrar_Destino_ROB(ROB, registro);
        if(TAG_ROB != -1 && ROB.get(TAG_ROB).etapa >= 3) { //Debemos asegurarnos de que haya finalizado la ejecución de la instrucción, sino usaremos un valor antiguo. COmo muy pronto estará dicha instrucción en etapa MEM en este ciclo, ejecutando la función COMMIT
            for (int j = 0; j < ER.size(); j++) { //Actualiza estación de reserva
                for (int k = 0; k < ER.get(j).size(); k++) {
                    if (ER.get(j).get(k).opa.compareTo(Integer.toString(TAG_ROB)) == 0) {
                        ER.get(j).get(k).opa = registro;
                        ER.get(j).get(k).opa_ok = 1;
                    }
                    if (ER.get(j).get(k).opb.compareTo(Integer.toString(TAG_ROB)) == 0) {
                        ER.get(j).get(k).opb = registro;
                        ER.get(j).get(k).opb_ok = 1;
                    }
                }
            }
        }
    }

    public static void Commit() { //TODO REVISAR BIEN
        ROB_t robT = ROB.get(p_rob_cabeza);
        if(robT.busy == 1 && robT.etapa == 3 && robT.valor_ok == 1 && robT.clk_tick_ok < ciclo){
            if(Integer.toString(robT.TAG_ROB).compareTo(robT.destino) == 0){
                banco_registros.get(Integer.parseInt(robT.destino)).valor = robT.valor; //Actualiza registro
            }if (robT.instruccion.cod.compareTo("sd") == 0){
                int est = Encontrar_Estacion(ER.get(1), robT.TAG_ROB); //Para obtener datos de estacion de reserva
                int pos_reg_rs = Encontrar_Registro(banco_registros, ER.get(1).get(est).opa); //Obtener el operando para sumar dir. memoria
                int pos_reg_rd = Encontrar_Registro(banco_registros, robT.destino);
                int dir_mem = ER.get(1).get(est).inmediato + banco_registros.get(pos_reg_rs).valor; //Dirección de memoria donde almacenar
                memoria_datos.set(dir_mem, banco_registros.get(pos_reg_rd).valor);
            } //memoria de datos
            if(act_rd != -1) {
                Actualizar_Registro_Usado(banco_registros.get(act_rd).contenido);
                act_rd = -1;
            }if(act_rs != -1){
                Actualizar_Registro_Usado(banco_registros.get(act_rs).contenido);
                act_rs = -1;
            }if (act_rt != -1){
                Actualizar_Registro_Usado(banco_registros.get(act_rt).contenido);
                act_rt = -1;
            }
            ROB.set(p_rob_cabeza, new ROB_t(p_rob_cabeza, null, 0, "", 0, 0,0, 4));
            p_rob_cabeza ++;
        }
    }
    public static void WB(){
        //int bucle = 0; //Controla salir del bucle cuando encuentra un resultado válido
        int tam_UF = UF.size();
        for (int i = 0; i < tam_UF; i++){
            if(UF.get(i).uso == 1 && UF.get(i).res_ok == 1 && UF.get(i).clk_tick_ok < ciclo) {
                int ent_rob = UF.get(i).TAG_ROB;
                int pos_reg_rd = Encontrar_Registro(banco_registros, ROB.get(ent_rob).destino); //Sirve también para ld y sd
                int pos_reg_rs = Encontrar_Registro(banco_registros, UF.get(i).opa); //Para la dirección de memoria
                String operacion = UF.get(i).operacion;

                if (operacion.compareTo("sd") == 0 || operacion.compareTo("ld") == 0){
                    if(Encontrar_Destinos_Previos(ROB, ent_rob, banco_registros.get(pos_reg_rd).contenido))
                        continue; //Si el registro que se va a almacenar es el mismo que el destino de una instrucción previa
                }
                ROB.get(ent_rob).valor = banco_registros.get(pos_reg_rd).valor;
                ROB.get(ent_rob).valor_ok = 1;
                ROB.get(ent_rob).clk_tick_ok = ciclo;
                UF.get(i).uso = 0; //Deja libre la UF
                Instruccion inst = memoria_instrucciones.get(ROB.get(ent_rob).TAG_ROB); //Utilizaremos esto para actualizar todos los registros de la instrucción
                act_rd = Encontrar_Registro(banco_registros, inst.rd);
                act_rt = Encontrar_Registro(banco_registros, inst.rt);
                act_rs = Encontrar_Registro(banco_registros, inst.rs);
                //for (int k = 0; k < tam_UF; k++) { //Bucle para recorrer todas las ER
                    for (int j = 0; j < ER.get(i).size(); j++) { //una iteración por línea de ER ocupada de ER[k]. Siempre empieza en 0 (posibilidad de que no sea así)
                        if(ER.get(i).get(j).busy == 1 && UF.get(i).TAG_ROB == ER.get(i).get(j).TAG_ROB){
                            if(pos_reg_rd != -1 && operacion.compareTo("sd") != 0)
                                banco_registros.get(pos_reg_rd).valor = UF.get(i).res;//Actualizar operando a (valor, ok y ciclo)
                            if (ER.get(i).get(j).opa_ok == 0 && ER.get(i).get(j).opa.compareTo(Integer.toString(UF.get(i).TAG_ROB)) == 0) { //si opa no disponible y depende de TAG_ROB
                                ER.get(i).get(j).opa_ok = 1;
                                ER.get(i).get(j).clk_tick_ok_a = ciclo;
                            } //opa
                            if (ER.get(i).get(j).opb_ok == 0 && ER.get(i).get(j).opb.compareTo(Integer.toString(UF.get(i).TAG_ROB)) == 0) { //si opa no disponible y depende de TAG_ROB
                                ER.get(i).get(j).opb_ok = 1;
                                ER.get(i).get(j).clk_tick_ok_b = ciclo;
                            } //opb
                            if(operacion.compareTo("ld") == 0) {
                                int dir_mem = ER.get(i).get(j).inmediato + banco_registros.get(pos_reg_rs).valor; //Dirección de memoria desde donde cargar el dato en el registro
                                banco_registros.get(pos_reg_rd).valor = memoria_datos.get(dir_mem);
                            } //memoria de datos carga
                            if(operacion.compareTo("sd") == 0){
                                int dir_mem = ER.get(i).get(j).inmediato + banco_registros.get(pos_reg_rs).valor;
                                memoria_datos.set(dir_mem, banco_registros.get(pos_reg_rd).valor);
                            } //memoria de datos almacenamiento
                            ER.get(i).get(j).busy = 0;
                        }
                    }
                UF.get(i).res_ok = 0;
                ROB.get(ent_rob).etapa = 3;
                break;

            }
        }

    }

    public static void EX(){
        int iniciado = 0; //Variable para saber si se ha iniciado la ejecución de una instrucción
        for(int i = 0; i < 3; i++) {
            UF_t unidad_func = UF.get(i);
            if (unidad_func.uso == 1) { //Mientras se ejecuta
                unidad_func.cont_ciclos++;
                switch (unidad_func.operacion) { //Según la operación acabará en un ciclo u otro
                    case ("add"), ("fadd"), ("sub"), ("fsub"): //add--rd--,--rs--,--rt //sub--rd--,--rs--,--rt
                        if (unidad_func.cont_ciclos >= 2) { //Si ha finalizado
                            int pos_reg_rs = Encontrar_Registro(banco_registros, unidad_func.opa);
                            int pos_reg_rt = Encontrar_Registro(banco_registros, unidad_func.opb);
                            unidad_func.res = banco_registros.get(pos_reg_rs).valor + banco_registros.get(pos_reg_rt).valor;
                            unidad_func.res_ok = 1;
                            unidad_func.clk_tick_ok = ciclo;
                            //int est = Encontrar_Estacion(ER.get(0), unidad_func.TAG_ROB);
                            //ER.get(0).get(est).busy = 0;
                        }
                        break;
                    case ("ld"), ("sd"): //ld--rt--,--inm--(--rs--) //sd--rt--,--inm--(--rs--)
                        if (unidad_func.cont_ciclos >= 3) {
                            unidad_func.res_ok = 1;
                            unidad_func.clk_tick_ok = ciclo;
                            //int est = Encontrar_Estacion(ER.get(1), unidad_func.TAG_ROB);
                            //ER.get(1).get(est).busy = 0;
                        }
                        break;
                    case ("mult"), ("fmult"): //mult--rd--,--rs
                        if (unidad_func.cont_ciclos >= 5) {
                            int pos_reg_rs = Encontrar_Registro(banco_registros, unidad_func.opa);
                            int pos_reg_rt = Encontrar_Registro(banco_registros, unidad_func.opb);
                            unidad_func.res = banco_registros.get(pos_reg_rs).valor * banco_registros.get(pos_reg_rt).valor;
                            unidad_func.res_ok = 1;
                            unidad_func.clk_tick_ok = ciclo;
                            //int est = Encontrar_Estacion(ER.get(2), unidad_func.TAG_ROB);
                            //ER.get(2).get(est).busy = 0;
                        }
                        break;
                }
                UF.set(i, unidad_func); //Guardar cambios
            }else { //Poner a ejecutar
                if (iniciado == 0) {
                    List<Estacion_reg_t> est_res = ER.get(i);
                    if (p_er_cola.get(i).size() != 0) { //Hay momentos donde no hayan instrucciones para rellenar una UF específica
                        int ent_ROB = p_er_cola.get(i).removeFirst(); //Se obtiene la primera entrada en ROB
                        int estacion = Encontrar_Estacion(est_res, ent_ROB); //Estacion específica en ER
                        int pos_reg_rd, pos_reg_rs, pos_reg_rt; //Consultar registros por si están siendo usados

                        if (i == 0) { //Operación en la ALU
                            if (ent_ROB != -1 && estacion != -1 && est_res.get(estacion).busy == 1) {
                                if (est_res.get(estacion).opa_ok == 1 && est_res.get(estacion).clk_tick_ok_a < ciclo
                                        && est_res.get(estacion).opb_ok == 1 && est_res.get(estacion).clk_tick_ok_b < ciclo) { //Comprobamos que todos los operandos están disponibles para operación ALU
                                    //enviar = 1;
                                    UF.get(i).uso = 1;
                                    UF.get(i).operacion = ROB.get(ent_ROB).instruccion.cod; //Para obtener el tipo de operación

                                    pos_reg_rs = Encontrar_Registro(banco_registros, est_res.get(estacion).opa);
                                    banco_registros.get(pos_reg_rs).ok = 0;
                                    banco_registros.get(pos_reg_rs).TAG_ROB = ent_ROB;

                                    pos_reg_rt = Encontrar_Registro(banco_registros, est_res.get(estacion).opb);
                                    banco_registros.get(pos_reg_rt).ok = 0;
                                    banco_registros.get(pos_reg_rt).TAG_ROB = ent_ROB;

                                    UF.get(i).opa = banco_registros.get(pos_reg_rs).contenido;
                                    UF.get(i).opb = banco_registros.get(pos_reg_rt).contenido;
                                    UF.get(i).cont_ciclos = 1;
                                    iniciado = 1;
                                } else {
                                    p_er_cola.get(i).addFirst(ent_ROB); //Volvemos a dejar la estación ya que aún no está lista para ejecutar
                                }
                            }
                        } else {
                            if (i == 2) { //Operación en MULT (Se puede juntar con ALU
                                if (ent_ROB != -1 && estacion != -1 && est_res.get(estacion).busy == 1) {
                                    if (est_res.get(estacion).opa_ok == 1 && est_res.get(estacion).clk_tick_ok_a < ciclo
                                            && est_res.get(estacion).opb_ok == 1 && est_res.get(estacion).clk_tick_ok_b < ciclo) { //Comprobamos que todos los operandos están disponibles para operación ALU
                                        //enviar = 1

                                        pos_reg_rs = Encontrar_Registro(banco_registros, est_res.get(estacion).opa);
                                        pos_reg_rt = Encontrar_Registro(banco_registros, est_res.get(estacion).opb);
                                        if(pos_reg_rs == -1 || pos_reg_rt == -1)
                                            break; //Si los datos aún no están listos

                                        UF.get(i).uso = 1;
                                        UF.get(i).operacion = ROB.get(ent_ROB).instruccion.cod; //Para obtener el tipo de operación

                                        banco_registros.get(pos_reg_rs).ok = 0;
                                        banco_registros.get(pos_reg_rs).TAG_ROB = ent_ROB;
                                        
                                        banco_registros.get(pos_reg_rt).ok = 0;
                                        banco_registros.get(pos_reg_rt).TAG_ROB = ent_ROB;

                                        UF.get(i).opa = banco_registros.get(pos_reg_rs).contenido;
                                        UF.get(i).opb = banco_registros.get(pos_reg_rt).contenido;
                                        UF.get(i).cont_ciclos = 1;
                                        iniciado = 1;
                                    } else {
                                        p_er_cola.get(i).addFirst(ent_ROB);
                                    }
                                }
                            } else { //Operación de almacenamiento o carga
                                if (ent_ROB != -1 && estacion != -1 && est_res.get(estacion).busy == 1) {
                                    if (est_res.get(estacion).operacion.compareTo("sd") == 0) {

                                        pos_reg_rs = Encontrar_Registro(banco_registros, est_res.get(estacion).opa);
                                        pos_reg_rt = Encontrar_Registro(banco_registros, ROB.get(ent_ROB).destino);
                                        if(pos_reg_rs == -1 || pos_reg_rt == -1)
                                            break; //Si los datos aún no están listos

                                        unidad_func.res = 0;

                                        banco_registros.get(pos_reg_rs).ok = 0;
                                        banco_registros.get(pos_reg_rs).TAG_ROB = ent_ROB;

                                        if (banco_registros.get(pos_reg_rt).ok == 1) { //Si rt no está siendo utilizado, significa que dicho valor no se tendrá que modificar hasta que el store se complete. SI esta siendo utilizado, en WB se esperará hasta que el valor sea actualizado
                                            banco_registros.get(pos_reg_rt).ok = 0;
                                            banco_registros.get(pos_reg_rt).TAG_ROB = ent_ROB;
                                        }
                                        UF.get(i).opa = banco_registros.get(pos_reg_rs).contenido;
                                        UF.get(i).cont_ciclos = 1;
                                        UF.get(i).uso = 1;
                                        UF.get(i).operacion = ROB.get(ent_ROB).instruccion.cod; //Para obtener el tipo de operación
                                        iniciado = 1;
                                    } else {
                                        if (est_res.get(estacion).operacion.compareTo("ld") == 0 && est_res.get(estacion).opa_ok == 1
                                                && est_res.get(estacion).clk_tick_ok_a < ciclo) {
                                            if (ROB.get(ent_ROB).destino.compareTo(UF.get(i).opa) == 0) {
                                                UF.get(i).res = ROB.get(ent_ROB).valor;
                                                UF.get(i).cont_ciclos = 3;
                                            } else {
                                                UF.get(i).cont_ciclos = 1;
                                            }
                                            pos_reg_rs = Encontrar_Registro(banco_registros, est_res.get(estacion).opa);
                                            //banco_registros.get(pos_reg_rs).ok = 0;
                                            //banco_registros.get(pos_reg_rs).TAG_ROB = ent_ROB;

                                            pos_reg_rt = Encontrar_Registro(banco_registros, ROB.get(ent_ROB).destino);
                                            if (banco_registros.get(pos_reg_rt).ok == 1) {
                                                banco_registros.get(pos_reg_rt).ok = 0;
                                                banco_registros.get(pos_reg_rt).TAG_ROB = ent_ROB;
                                            }

                                            UF.get(i).opa = banco_registros.get(pos_reg_rs).contenido;
                                            UF.get(i).uso = 1;
                                            UF.get(i).operacion = ROB.get(ent_ROB).instruccion.cod;
                                            iniciado = 1;
                                        } else {
                                            p_er_cola.get(i).addFirst(ent_ROB);
                                        }
                                    }
                                }
                            }
                        }
                        if (iniciado == 1) {
                            UF.get(i).TAG_ROB = ent_ROB;
                            ROB.get(est_res.get(estacion).TAG_ROB).etapa = 2;
                        }
                    }
                }
            }
        }


    }

    public static void ISS() {
        if(PC < inst_programa) { //Si no quedan más instrucciones que iniciar, no se realiza esta función
            Instruccion instruccion = memoria_instrucciones.get(PC);
            String cod = instruccion.cod;
            if (cod.compareTo("") != 0) {
                Estacion_reg_t estacionRegT = new Estacion_reg_t(1, cod, "-", 0, 0, "-", 0, 0, instruccion.inmediato, 0);
                int puntero_entrada_rob = Encontrar_Entrada_ROB(ROB, instruccion);
                int pos_reg_rd = Encontrar_Registro(banco_registros, instruccion.rd); //Posicion del registro destino en el banco de registros
                int pos_reg_rs = Encontrar_Registro(banco_registros, instruccion.rs);
                int pos_reg_rt = Encontrar_Registro(banco_registros, instruccion.rt);
                ROB_t entrada_ROB = ROB.get(puntero_entrada_rob);
                if (puntero_entrada_rob == -1) { //-1 si no ha encontrado dicha entrada
                    entrada_ROB = new ROB_t(p_rob_cola, instruccion, 1, "-", 0, 0, -1, 1);
                }
                if (entrada_ROB != null) {//Actualizar ROB
                    if (pos_reg_rd != -1) {
                        if(banco_registros.get(pos_reg_rd).TAG_ROB == -1) //Si aún nadie ha usado este registro
                            banco_registros.get(pos_reg_rd).TAG_ROB = puntero_entrada_rob; //Indicamos que a ese registro se le debe actualizar el valor
                        banco_registros.get(pos_reg_rd).ok = 0;
                    }else{ //Para ld y sd
                        if(banco_registros.get(pos_reg_rt).TAG_ROB == -1) //Si aún nadie ha usado este registro
                            banco_registros.get(pos_reg_rt).TAG_ROB = puntero_entrada_rob; //Indicamos que a ese registro se le debe actualizar el valor
                        banco_registros.get(pos_reg_rt).ok = 0;
                    }
                    estacionRegT.TAG_ROB = p_rob_cola; //Actualiza etiqueta de la línea ROB en estación de reserva donde se ha almacenado la instrucción
                    entrada_ROB.etapa = 0;
                    switch (cod) { //Actualizar vector de punteros de UF, apunta a la cola de ER[tipo] para almacenarla en esa posición y entrada ROB //TODO REVISAR p_er_cola
                        case ("add"), ("fadd"), ("sub"), ("fsub"): //add--rd--,--rs--,--rt //sub--rd--,--rs--,--rt
                            entrada_ROB.destino = instruccion.rd; //ACtualiza destino en ROB
                            //p_er_cola.get(0).add(p_rob_cola); //Añade ROB a la cola de espera de la UF necesitada
                            p_er_cola.get(0).addLast(p_rob_cola); //Añadimos el número de entrada ROB para la cola de estaciones de registro (FIFO)
                            ROB.set(p_rob_cola, entrada_ROB); //Se añade la entrada en ROB
                            if (banco_registros.get(pos_reg_rs).ok == 1 && banco_registros.get(pos_reg_rs).clk_tick_ok < ciclo) {
                                estacionRegT.opa = banco_registros.get(pos_reg_rs).contenido;
                                estacionRegT.clk_tick_ok_a = ciclo;
                                estacionRegT.opa_ok = 1;

                            } else {
                                int posible_dest_rs = Encontrar_Destino_ROB(ROB, banco_registros.get(pos_reg_rs).contenido);
                                if(posible_dest_rs != -1){ //Aunque sean utilizados por otras instrucciones, mientras no se modifiquen no pasa nada
                                    estacionRegT.opa = Integer.toString(posible_dest_rs);
                                    estacionRegT.opa_ok = 0;
                                }else {
                                    estacionRegT.opa = banco_registros.get(pos_reg_rs).contenido;
                                    estacionRegT.clk_tick_ok_a = ciclo;
                                    estacionRegT.opa_ok = 1;
                                }
                            }
                            if (banco_registros.get(pos_reg_rt).ok == 1 && banco_registros.get(pos_reg_rt).clk_tick_ok < ciclo) {
                                estacionRegT.opb = banco_registros.get(pos_reg_rt).contenido;
                                estacionRegT.clk_tick_ok_b = ciclo;
                                estacionRegT.opb_ok = 1;
                            } else {
                                int posible_dest_rt = Encontrar_Destino_ROB(ROB, banco_registros.get(pos_reg_rt).contenido);
                                if (posible_dest_rt != -1) { //Aunque sean utilizados por otras instrucciones, mientras no se modifiquen no pasa nada
                                    estacionRegT.opb = Integer.toString(posible_dest_rt);
                                    estacionRegT.opb_ok = 0;
                                } else {
                                    estacionRegT.opb = banco_registros.get(pos_reg_rs).contenido;
                                    estacionRegT.clk_tick_ok_b = ciclo;
                                    estacionRegT.opb_ok = 1;
                                }
                            }
                            ER.get(0).add(estacionRegT); //Guardar linea en estación de reserva e la UF correspondiente
                            break;
                        case ("ld"): //ld--rt--,--inm--(--rs--)
                            p_er_cola.get(1).addLast(p_rob_cola);
                            entrada_ROB.destino = instruccion.rt;
                            ROB.set(p_rob_cola, entrada_ROB);
                            if (banco_registros.get(pos_reg_rs).ok == 1 && banco_registros.get(pos_reg_rs).clk_tick_ok < ciclo) {
                                estacionRegT.opa = banco_registros.get(pos_reg_rs).contenido;
                                estacionRegT.clk_tick_ok_a = ciclo;
                                estacionRegT.opa_ok = 1;
                            } else {
                                int posible_dest_rs = Encontrar_Destino_ROB(ROB, banco_registros.get(pos_reg_rs).contenido);
                                if(posible_dest_rs != -1){ //Aunque sean utilizados por otras instrucciones, mientras no se modifiquen no pasa nada
                                    estacionRegT.opa = Integer.toString(posible_dest_rs);
                                    estacionRegT.opa_ok = 0;
                                }else {
                                    estacionRegT.opa = banco_registros.get(pos_reg_rs).contenido;
                                    estacionRegT.clk_tick_ok_a = ciclo;
                                    estacionRegT.opa_ok = 1;
                                }
                            }
                            ER.get(1).add(estacionRegT);
                            break;
                        case ("sd"): //sd--rt--,--inm--(--rs--)
                            entrada_ROB.destino = instruccion.rt;
                            p_er_cola.get(1).addLast(p_rob_cola);
                            ROB.set(p_rob_cola, entrada_ROB);
                            if (banco_registros.get(pos_reg_rs).ok == 1 && banco_registros.get(pos_reg_rs).clk_tick_ok < ciclo) {
                                estacionRegT.opa = banco_registros.get(pos_reg_rs).contenido;
                                estacionRegT.clk_tick_ok_a = ciclo;
                                estacionRegT.opa_ok = 1;
                            } else {
                                int posible_dest_rs = Encontrar_Destino_ROB(ROB, banco_registros.get(pos_reg_rs).contenido);
                                if(posible_dest_rs != -1){ //Aunque sean utilizados por otras instrucciones, mientras no se modifiquen no pasa nada
                                    estacionRegT.opa = Integer.toString(posible_dest_rs);
                                    estacionRegT.opa_ok = 0;
                                }else {
                                    estacionRegT.opa = banco_registros.get(pos_reg_rs).contenido;
                                    estacionRegT.clk_tick_ok_a = ciclo;
                                    estacionRegT.opa_ok = 1;
                                }
                            }
                            ER.get(1).add(estacionRegT);
                            break;
                        case ("mult"), ("fmult"): //mult--rd--,--rt--,--rs
                            entrada_ROB.destino = instruccion.rd;
                            //p_er_cola.get(2).add(p_er_cola.get(2).size());
                            p_er_cola.get(2).addLast(p_rob_cola);
                            ROB.set(p_rob_cola, entrada_ROB);
                            if (banco_registros.get(pos_reg_rs).ok == 1 && banco_registros.get(pos_reg_rs).clk_tick_ok < ciclo) {
                                estacionRegT.opa = banco_registros.get(pos_reg_rs).contenido;
                                estacionRegT.clk_tick_ok_a = ciclo;
                                estacionRegT.opa_ok = 1;
                            } else {
                                int posible_dest_rs = Encontrar_Destino_ROB(ROB, banco_registros.get(pos_reg_rs).contenido);
                                if(posible_dest_rs != -1){ //Aunque sean utilizados por otras instrucciones, mientras no se modifiquen no pasa nada
                                    estacionRegT.opa = Integer.toString(posible_dest_rs);
                                    estacionRegT.opa_ok = 0;
                                }else {
                                    estacionRegT.opa = banco_registros.get(pos_reg_rs).contenido;
                                    estacionRegT.clk_tick_ok_a = ciclo;
                                    estacionRegT.opa_ok = 1;
                                }
                            }
                            if (banco_registros.get(pos_reg_rt).ok == 1 && banco_registros.get(pos_reg_rt).clk_tick_ok < ciclo) {
                                estacionRegT.opb = banco_registros.get(pos_reg_rt).contenido;
                                estacionRegT.clk_tick_ok_b = ciclo;
                                estacionRegT.opb_ok = 1;
                            } else {
                                int posible_dest_rt = Encontrar_Destino_ROB(ROB, banco_registros.get(pos_reg_rt).contenido);
                                if (posible_dest_rt != -1) { //Aunque sean utilizados por otras instrucciones, mientras no se modifiquen no pasa nada
                                    estacionRegT.opb = Integer.toString(posible_dest_rt);
                                    estacionRegT.opb_ok = 0;
                                } else {
                                    estacionRegT.opb = banco_registros.get(pos_reg_rs).contenido;
                                    estacionRegT.clk_tick_ok_b = ciclo;
                                    estacionRegT.opb_ok = 1;
                                }
                            }
                            ER.get(2).add(estacionRegT);
                            break;
                    }
                    ROB.get(p_rob_cola).busy = 1;
                    ROB.get(p_rob_cola).etapa = 1;
                    p_rob_cola++;
                    PC++;

                }
            }
        }
    }

    public static void Mostrar_Por_Pantalla(){
        System.out.print("C" + ciclo + "\t");
        for(int i = 0; i < ROB.size(); i++){ //Bucle para ISS
            if(ROB.get(i).etapa == 1){
                System.out.print("I" + i + " ---> ISS \t");
            }
        }for(int i = 0; i < ROB.size(); i++){ //Bucle para EX
            if(ROB.get(i).etapa == 2){
                int uf = Encontrar_UF_con_linea_ROB(UF, ROB.get(i).TAG_ROB); //Encontrar la UF con dicha TAG_ROB
                if(UF.get(uf).operacion.compareTo("mult") == 0 || UF.get(uf).operacion.compareTo("fmult") == 0) {
                    if (UF.get(uf).cont_ciclos < 6 && uf != -1)
                        System.out.print("I" + i + " ---> EX" + UF.get(uf).cont_ciclos + "\t");
                }else {
                    if (UF.get(uf).cont_ciclos < 4 && uf != -1) // Cambiar por límite preestablecido
                        System.out.print("I" + i + " ---> EX" + UF.get(uf).cont_ciclos + "\t");
                }
            }
        }for(int i = 0; i < ROB.size(); i++){ //Bucle para MEM
            if(ROB.get(i).etapa == 3){
                System.out.print("I" + i + " ---> MEM \t");
                break;
            }
        }for(int i = 0; i < ROB.size(); i++){ //Bucle para Commit
            if(ROB.get(i).etapa == 4){
                System.out.print("I" + i + " ---> Commit \t");
                ROB.get(i).etapa = -1;
                fin_programa --;
                break;
            }
        }System.out.println("\n");

        System.out.println("------------------------------------------------------- Contenido Registros -------------------------------------------------------");
        for(int i = 0; i < banco_registros.size(); i++){
            System.out.print("Registro: " + banco_registros.get(i).contenido + "(Disponible: " + banco_registros.get(i).ok + ") --> contenido: " + banco_registros.get(i).valor + " | ");
            if(i % 5 == 0)
                System.out.print("\n");
        }
        System.out.println("\n");

        System.out.println("----------------------------------------------------- Contenido Memoria Datos -----------------------------------------------------");
        for (int i = 0; i < memoria_datos.size(); i++){
            System.out.print("Entrada: " + i + " --> contenido: " + memoria_datos.get(i) + " | ");
            if(i % 5 == 0)
                System.out.print("\n");
        }
        System.out.println("\n");


        System.out.println("---------------------------------------------------------- Contenido ER ----------------------------------------------------------");
        int contador = 0;
        System.out.println(" \t\t| busy \t| operacion | opa \t| opa_ok \t| clk_tick_ok_a | opb \t| opb_ok \t| clk_tick_ok_b \t| inmediato \t| TAG_ROB");
        List <Estacion_reg_t> ALU = ER.get(0);
        for(Estacion_reg_t estacionRegT: ALU){
            System.out.print("ALU " + contador + " \t| ");
            System.out.println(estacionRegT.busy + " \t| " + estacionRegT.operacion + " \t\t| " + estacionRegT.opa  + " \t| " + estacionRegT.opa_ok
                    + " \t\t| " + estacionRegT.clk_tick_ok_a + " \t\t\t| " + estacionRegT.opb + " \t| " + estacionRegT.opb_ok + " \t\t| "
                    + estacionRegT.clk_tick_ok_b + " \t\t\t\t| " + estacionRegT.inmediato + " \t\t\t| " + estacionRegT.TAG_ROB);
            contador ++;
        }contador = 0;
        List <Estacion_reg_t> MEM = ER.get(1);
        for(Estacion_reg_t estacionRegT: MEM){
            System.out.print("MEM " + contador + " \t| ");
            System.out.println(estacionRegT.busy + " \t| " + estacionRegT.operacion + " \t\t| " + estacionRegT.opa  + " \t| " + estacionRegT.opa_ok
                    + " \t\t| " + estacionRegT.clk_tick_ok_a + " \t\t\t| " + estacionRegT.opb + " \t| " + estacionRegT.opb_ok + " \t\t| "
                    + estacionRegT.clk_tick_ok_b + " \t\t\t\t| " + estacionRegT.inmediato + " \t\t\t| " + estacionRegT.TAG_ROB);
            contador ++;
        }contador = 0;
        List <Estacion_reg_t> MULT = ER.get(2);
        for(Estacion_reg_t estacionRegT: MULT){
            System.out.print("MULT " + contador + " \t| ");
            System.out.println(estacionRegT.busy + "\t\t| " + estacionRegT.operacion + " \t| " + estacionRegT.opa  + " \t| " + estacionRegT.opa_ok
                    + " \t\t| " + estacionRegT.clk_tick_ok_a + " \t\t\t| " + estacionRegT.opb + " \t| " + estacionRegT.opb_ok + " \t\t| "
                    + estacionRegT.clk_tick_ok_b + " \t\t\t\t| " + estacionRegT.inmediato + " \t\t\t| " + estacionRegT.TAG_ROB);
            contador ++;
        }

        System.out.println("\n");
        System.out.println("---------------------------------------------------------- Contenido ROB ----------------------------------------------------------");
        System.out.println("TAG | instruccion \t\t| busy \t| dest \t\t| valor | valor_ok \t| clk_tick_ok \t| etapa");
        List <ROB_t> rob = ROB;
        for(ROB_t ent_rob: rob){
            if(ent_rob.etapa != -1 && ent_rob.instruccion != null) {
                String inst = Mostrar_Instruccion(ent_rob.instruccion);
                if (ent_rob.instruccion.cod.compareTo("ld") == 0 || ent_rob.instruccion.cod.compareTo("sd") == 0)
                    System.out.println(ent_rob.TAG_ROB + " \t| " + inst + "\t\t| " + ent_rob.busy + " \t| " + ent_rob.destino
                            + "\t\t| " + ent_rob.valor + " \t| " + ent_rob.valor_ok + " \t\t| " + ent_rob.clk_tick_ok + " \t\t\t| " + ent_rob.etapa);
                else
                    System.out.println(ent_rob.TAG_ROB + " \t| " + inst + " \t| " + ent_rob.busy + " \t| " + ent_rob.destino
                            + "\t\t| " + ent_rob.valor + " \t| " + ent_rob.valor_ok + " \t\t| " + ent_rob.clk_tick_ok + " \t\t\t| " + ent_rob.etapa);
            }
        }
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------");

        System.out.println("\n");
        System.out.println("---------------------------------------------------------- Contenido UF ----------------------------------------------------------");
        System.out.println("\t| uso \t| cont_ciclos \t| TAG_ROB \t| opa \t| opb \t| operacion \t| res \t\t| res_ok \t| clk_tick_ok ");

        for(int i = 0; i < UF.size(); i++){
            UF_t uf = UF.get(i);
            System.out.println(i + "\t| " + uf.uso + " \t| " + uf.cont_ciclos + " \t\t\t| " + uf.TAG_ROB + " \t\t| " + uf.opa + " \t| " + uf.opb +
                    "\t| " + uf.operacion + "\t\t\t| " + uf.res + " \t\t| " + uf.res_ok + " \t\t| " + uf.clk_tick_ok);
        }
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------");

        System.out.println("##################################################################################################################################");
        System.out.println("\n");
    }


    public static String Mostrar_Instruccion(Instruccion instruccion){
        String inst = instruccion.cod;
        switch (instruccion.cod){
            case("add"), ("sub"), ("fadd"), ("fsub"), ("mult"), ("fmult"): //add--rd--,--rs--,--rt //sub--rd--,--rs--,--rt //mult--rd--,--rt--,--rs
                inst += " " + instruccion.rd + ", " + instruccion.rs + ", " + instruccion.rt;
                break;
            case("ld"), ("sd"): //ld--rt--,--inm--(--rs--) //sd--rt--,--inm--(--rs--)
                inst += " " + instruccion.rt + ", " + instruccion.inmediato + "(" + instruccion.rs + ")";
                break;
        }return inst;
    }

}