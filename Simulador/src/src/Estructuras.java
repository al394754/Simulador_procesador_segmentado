package src;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Estructuras {
    public static class Instruccion { //Instrucción
        String cod; //Operación a realizar
        String rd; //Registro destino
        String rs; //Registro fuente op1
        String rt; //Registro fuente op2
        int inmediato; //Dato inmediato

        public Instruccion(String cod, String rd, String rs, String rt, int inmediato) {
            this.cod = cod;
            this.rd = rd;
            this.rs = rs;
            this.rt = rt;
            this.inmediato = inmediato;
        }
    }

    public static class Reg_t{ //Registro del banco de registros
        String contenido; //Nombre
        int valor;
        int ok; //Contenido valido (1) o no (0)
        int clk_tick_ok; //Ciclo de reloj cunado se valida ok
        int TAG_ROB; //Si ok es 0, etiqueta de la línea de ROB donde está almacenada la instrucción que lo actualizará. Almacena la última


        public Reg_t(String contenido, int valor, int ok, int clk_tick_ok, int TAG_ROB) {
            this.contenido = contenido;
            this.valor = valor;
            this.ok = ok;
            this.clk_tick_ok = clk_tick_ok;
            this.TAG_ROB = TAG_ROB;
        }
    }

    public static class Estacion_reg_t{ //Línea de una estación de reserva
        int busy; //contenido de la línea válido (1) o no (0)
        String operacion; //operación a realizar en UF (suma,resta,mult,lw,sw)
        String opa; //valor Vj
        int opa_ok;  //Qj, (1) opA válido o no (0)
        int clk_tick_ok_a; //ciclo de reloj donde se valida opa_ok
        String opb; //Valor Vk
        int opb_ok; //Qk, (1) válido o (0) no válido
        int clk_tick_ok_b; //ciclo de reloj se valida donde opb_ok
        int inmediato; //utilizado para las instrucciones lw/sw
        int TAG_ROB; //etiqueta de la línea del ROB donde se ha almacenado esta instrucción

        public Estacion_reg_t(int busy, String operacion, String opa, int opa_ok, int clk_tick_ok_a, String opb, int opb_ok, int clk_tick_ok_b, int inmediato, int TAG_ROB) {
            this.busy = busy;
            this.operacion = operacion;
            this.opa = opa;
            this.opa_ok = opa_ok;
            this.clk_tick_ok_a = clk_tick_ok_a;
            this.opb = opb;
            this.opb_ok = opb_ok;
            this.clk_tick_ok_b = clk_tick_ok_b;
            this.inmediato = inmediato;
            this.TAG_ROB = TAG_ROB;
        }
    }

    public static class ROB_t{ //Línea de ROB
        int TAG_ROB; //Etiqueta que identifica la línea de ROB
        Instruccion instruccion; //tipo de instrucción
        int busy; //Indica si el contenido de la línea es válido (1) o no (0)
        String destino; //identificador registro destino (rd)
        int valor; //resultado tras finalizar la etapa EX
        int valor_ok; //indica si valor es válido (1) o no (0)
        int clk_tick_ok; //ciclo de reloj cuando se valida valor_ok
        int etapa; //Etapa de procesamiento de la instrucción ISS, EX, WB


        public ROB_t(int TAG_ROB, Instruccion instruccion, int busy, String destino, int valor, int valor_ok, int clk_tick_ok, int etapa) {
            this.TAG_ROB = TAG_ROB;
            this.instruccion = instruccion;
            this.busy = busy;
            this.destino = destino;
            this.valor = valor;
            this.valor_ok = valor_ok;
            this.clk_tick_ok = clk_tick_ok;
            this.etapa = etapa;
        }
    }

    public static class UF_t{ //Simula funcionamiento de una UF
        int uso; //Indica si UF está utilizada (1) o no (0)
        int cont_ciclos; // Indica ciclos consumidos por la UF
        int TAG_ROB; //Línea del ROB donde se tiene que almacenar el resultado tras EX
        String opa; //Valor opa (en almacenamiento contiene dato a escribir en memoria
        String opb; //Valor opb (en lw y sw contiene dirección de memoria de datos )
        String operacion; //Se utiliza para indicar operacion a realizar add/sub y lw/sw o mult
        int res; //Resultado
        int res_ok; //resultado valido (1)
        int clk_tick_ok; //ciclo de reloj cuando se valida res_ok


        public UF_t(int uso, int cont_ciclos, int TAG_ROB, String opa, String opb, String operacion, int res, int res_ok, int clk_tick_ok) {
            this.uso = uso;
            this.cont_ciclos = cont_ciclos;
            this.TAG_ROB = TAG_ROB;
            this.opa = opa;
            this.opb = opb;
            this.operacion = operacion;
            this.res = res;
            this.res_ok = res_ok;
            this.clk_tick_ok = clk_tick_ok;
        }
    }
}
