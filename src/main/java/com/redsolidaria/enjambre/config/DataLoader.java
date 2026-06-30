package com.redsolidaria.enjambre.config;

import com.redsolidaria.enjambre.model.Administrador;
import com.redsolidaria.enjambre.model.Curso;
import com.redsolidaria.enjambre.model.Pregunta;
import com.redsolidaria.enjambre.repository.AdministradorRepository;
import com.redsolidaria.enjambre.repository.CursoRepository;
import com.redsolidaria.enjambre.repository.PreguntaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private PreguntaRepository preguntaRepository;

    @Override
    public void run(String... args) throws Exception {
        
        // 1. Verificar si ya existe un administrador
        if (!administradorRepository.existsByEmail("admin@redsolidaria.pe")) {
            
            // Crear administrador principal
            Administrador admin = new Administrador(
                "Administrador", 
                "Sistema", 
                "admin@redsolidaria.pe", 
                "admin123"
            );
            
            administradorRepository.save(admin);
            
            System.out.println("========================================");
            System.out.println("✅ CUENTA ADMIN CREADA AUTOMÁTICAMENTE:");
            System.out.println("   Email: admin@redsolidaria.pe");
            System.out.println("   Contraseña: admin123");
            System.out.println("   Rol: ADMIN");
            System.out.println("========================================");
        } else {
            System.out.println("ℹ️ La cuenta admin ya existe en la base de datos");
        }
        
        // 2. Cargar datos iniciales de Cursos y Preguntas si la tabla está vacía
        if (cursoRepository.count() == 0) {
            System.out.println("🌱 Cargando cursos y preguntas de capacitación iniciales...");

            // Curso 1
            Curso curso1 = new Curso(
                "Lenguaje de Señas Peruano - Nivel 1",
                "Curso completo de introducción al lenguaje de señas en el contexto peruano.",
                "Básico",
                1,
                "https://youtu.be/2FOgfDn5rkg"
            );
            cursoRepository.save(curso1);

            List<Pregunta> preguntasCurso1 = new ArrayList<>();
            preguntasCurso1.add(new Pregunta(curso1, "¿Cuál es el principal objetivo del Lenguaje de Señas Peruano (LSP)?", 
                "Enseñar un idioma extranjero.", 
                "Facilitar la comunicación con personas sordas o con discapacidad auditiva.", 
                "Reemplazar al idioma español en Perú.", 
                "Ser utilizado únicamente en el ámbito educativo.", "b"));

            preguntasCurso1.add(new Pregunta(curso1, "El Lenguaje de Señas Peruano es un sistema de comunicación que se basa principalmente en:", 
                "Sonidos y tonos de voz.", 
                "Gestos manuales, expresiones faciales y movimiento corporal.", 
                "Palabras escritas en un papel.", 
                "Códigos binarios.", "b"));

            preguntasCurso1.add(new Pregunta(curso1, "La expresión facial en el Lenguaje de Señas:", 
                "No es importante, solo importan las manos.", 
                "Es un componente fundamental, ya que aporta información gramatical y emocional.", 
                "Solo se usa para mostrar sorpresa.", 
                "Es diferente en cada región del Perú.", "b"));

            preguntasCurso1.add(new Pregunta(curso1, "¿Qué implica la \"dactilología\" en el contexto del LSP?", 
                "Es el uso de objetos para comunicarse.", 
                "Es la representación manual de las letras del abecedario para deletrear palabras.", 
                "Es una forma de bailar.", 
                "Es un tipo de señas exclusivo para números.", "b"));

            preguntasCurso1.add(new Pregunta(curso1, "La gramática del Lenguaje de Señas Peruano:", 
                "Es idéntica a la gramática del español.", 
                "Tiene su propia estructura y reglas, diferentes a las del español hablado.", 
                "No tiene reglas gramaticales.", 
                "Se basa en el inglés.", "b"));

            preguntasCurso1.add(new Pregunta(curso1, "Al igual que los idiomas hablados, el LSP:", 
                "Es un invento moderno sin variaciones.", 
                "Puede tener variaciones y dialectos regionales dentro del propio Perú.", 
                "Se habla de la misma forma en todo el mundo.", 
                "Es un código secreto.", "b"));

            preguntasCurso1.add(new Pregunta(curso1, "Las \"señas\" en el LSP se clasifican principalmente según:", 
                "La edad de la persona que las usa.", 
                "Su forma, movimiento, ubicación y orientación de la mano.", 
                "El color de la ropa del hablante.", 
                "La hora del día.", "b"));

            preguntasCurso1.add(new Pregunta(curso1, "Para comunicarse de manera efectiva con una persona sorda que usa LSP, es más adecuado:", 
                "Gritar para que pueda escuchar mejor.", 
                "Hablarle de espaldas.", 
                "Mantener contacto visual y utilizar las señas de forma clara.", 
                "Usar solo la dactilología para todo.", "c"));

            preguntasCurso1.add(new Pregunta(curso1, "El Lenguaje de Señas Peruano es reconocido en el Perú como:", 
                "Un simple sistema de gestos sin importancia oficial.", 
                "La lengua natural de la comunidad sorda peruana.", 
                "Un método anticuado que está en desuso.", 
                "Una asignatura escolar obligatoria.", "b"));

            preguntasCurso1.add(new Pregunta(curso1, "¿Qué se necesita para aprender LSP de manera efectiva?", 
                "Solo leer un libro sobre el tema.", 
                "Práctica constante e inmersión en la comunidad de señas.", 
                "Tener buena voz.", 
                "Un traductor automático.", "b"));
            
            preguntaRepository.saveAll(preguntasCurso1);


            // Curso 2
            Curso curso2 = new Curso(
                "Asistencia a Personas con Discapacidad Motriz",
                "Aprende técnicas seguras para asistir a personas con movilidad reducida y respetar su autonomía.",
                "Intermedio",
                2,
                "https://youtu.be/GQMUisKnrEo"
            );
            cursoRepository.save(curso2);

            List<Pregunta> preguntasCurso2 = new ArrayList<>();
            preguntasCurso2.add(new Pregunta(curso2, "Cuando se ayuda a una persona en silla de ruedas a moverse, ¿qué se debe hacer?", 
                "Tomarla del respaldo y levantarla bruscamente.", 
                "Sujetar firmemente el manillar, avisar antes de moverse y evitar movimientos bruscos.", 
                "Empujar la silla rápidamente para que la persona se sienta emocionada.", 
                "No es necesario preguntar, solo hay que hacerlo rápido.", "b"));

            preguntasCurso2.add(new Pregunta(curso2, "La \"regla de oro\" al ofrecer ayuda a una persona con discapacidad motriz es:", 
                "Ayudarla siempre, sin importar lo que diga, \"por su bien\".", 
                "Esperar a que la persona solicite la ayuda, para respetar su autonomía.", 
                "Ayudarla solo si es un familiar.", 
                "Nunca ofrecer ayuda.", "b"));

            preguntasCurso2.add(new Pregunta(curso2, "Al hablar con una persona que utiliza silla de ruedas, es recomendable:", 
                "Ponerse a su misma altura para que el contacto visual sea más cómodo.", 
                "Hablarle desde arriba para que te vea mejor.", 
                "Gritarle para que te escuche bien.", 
                "Evitar mirarla a los ojos.", "a"));

            preguntasCurso2.add(new Pregunta(curso2, "¿Qué son las barreras arquitectónicas?", 
                "Obstáculos físicos en el entorno (escalones, puertas estrechas) que dificultan la movilidad.", 
                "Un tipo de muleta especial.", 
                "Un juego de mesa.", 
                "Solo las rampas muy empinadas.", "a"));

            preguntasCurso2.add(new Pregunta(curso2, "¿Qué actitud es fundamental al asistir a una persona con discapacidad motriz?", 
                "Actuar con lástima y sobreprotección.", 
                "Usar un lenguaje infantilizado para que se sienta mejor.", 
                "Actuar con naturalidad, respeto y empatía, enfocándose en la persona, no en su limitación.", 
                "Ignorarla para no incomodarla.", "c"));

            preguntasCurso2.add(new Pregunta(curso2, "¿Por qué es importante preguntar cómo desea ser ayudada una persona con discapacidad motriz?", 
                "Porque cada persona tiene sus propias necesidades y preferencias, y así se respeta su autonomía.", 
                "Porque es una formalidad sin importancia.", 
                "Porque todas las personas con discapacidad motriz necesitan el mismo tipo de ayuda.", 
                "Para saber si habla español.", "a"));

            preguntasCurso2.add(new Pregunta(curso2, "Al indicar una dirección a una persona con discapacidad motriz, ¿qué es útil hacer?", 
                "Señalar a lo lejos y no dar más detalles.", 
                "Advertirle sobre posibles obstáculos y distancias en el camino.", 
                "Decirle que es mejor que no vaya.", 
                "Indicar únicamente el nombre de la calle.", "b"));

            preguntasCurso2.add(new Pregunta(curso2, "El término \"discapacidad motriz\" se refiere a:", 
                "Una dificultad para aprender matemáticas.", 
                "Una alteración en la capacidad de movimiento y/o coordinación del cuerpo.", 
                "Un problema de visión.", 
                "La falta de habilidades sociales.", "b"));

            preguntasCurso2.add(new Pregunta(curso2, "Las ayudas técnicas como sillas de ruedas, bastones o muletas son:", 
                "Accesorios prescindibles que se pueden dejar de lado.", 
                "Elementos imprescindibles para la autonomía de la persona, y deben estar siempre a su alcance.", 
                "Un estorbo en el día a día.", 
                "Solo para deportes.", "b"));

            preguntasCurso2.add(new Pregunta(curso2, "En el transporte público, ¿cómo se debe priorizar a una persona con discapacidad motriz?", 
                "No es necesario darle prioridad.", 
                "Facilitarle el acceso y el espacio, respetando los asientos y áreas prioritarias.", 
                "Pedirle que espere a que todos los demás suban.", 
                "Ignorarla.", "b"));

            preguntaRepository.saveAll(preguntasCurso2);


            // Curso 3
            Curso curso3 = new Curso(
                "Primeros Auxilios",
                "Curso esencial de primeros auxilios y soporte vital básico para situaciones de emergencia.",
                "Básico",
                4,
                "https://youtu.be/MYEnE0bJLXg"
            );
            cursoRepository.save(curso3);

            List<Pregunta> preguntasCurso3 = new ArrayList<>();
            preguntasCurso3.add(new Pregunta(curso3, "La \"Conducta PAS\" en primeros auxilios significa:", 
                "Preguntar, Avisar, Socorrer.", 
                "Proteger, Alertar, Socorrer.", 
                "Parar, Ayudar, Salvar.", 
                "Prevenir, Actuar, Sanar.", "b"));

            preguntasCurso3.add(new Pregunta(curso3, "¿Cuál es el primer paso que debe realizar un socorrista al llegar a la escena de un accidente?", 
                "Realizar la RCP de inmediato.", 
                "Asegurarse de que la escena es segura para él y para la víctima.", 
                "Mover a la víctima para ponerla cómoda.", 
                "Llamar a la familia de la víctima.", "b"));

            preguntasCurso3.add(new Pregunta(curso3, "La Reanimación Cardiopulmonar (RCP) es una técnica que se aplica cuando:", 
                "Una persona tiene fiebre.", 
                "Una persona no respira o no tiene pulso.", 
                "Una persona se ha roto un hueso.", 
                "Una persona tiene un dolor de cabeza.", "b"));

            preguntasCurso3.add(new Pregunta(curso3, "Ante una hemorragia grave, ¿cuál es una de las primeras medidas a tomar?", 
                "Aplicar hielo directamente sobre la herida.", 
                "Ejercer presión directa sobre la herida con un apósito o tela limpia.", 
                "Colocar a la víctima en posición de shock.", 
                "Darle de beber agua.", "b"));

            preguntasCurso3.add(new Pregunta(curso3, "El Soporte Vital Básico (SVB) incluye técnicas como:", 
                "Realizar una cirugía de emergencia.", 
                "Mantener la vía aérea permeable, la respiración y la circulación (RCP).", 
                "Administrar medicamentos intravenosos.", 
                "Tomar la temperatura.", "b"));

            preguntasCurso3.add(new Pregunta(curso3, "¿Qué se debe hacer en caso de que una persona esté sufriendo una obstrucción de la vía aérea por un objeto (atragantamiento)?", 
                "Darle golpes en la espalda sin más.", 
                "Aplicar la maniobra de Heimlich (compresiones abdominales).", 
                "Meter los dedos en la boca de la persona sin mirar.", 
                "Acostar a la persona boca arriba.", "b"));

            preguntasCurso3.add(new Pregunta(curso3, "Ante una quemadura, ¿qué acción NO es correcta?", 
                "Enfriar la zona quemada con agua del grifo durante varios minutos.", 
                "Cubrir la quemadura con un paño limpio y húmedo.", 
                "Aplicar hielo directamente sobre la quemadura.", 
                "Retirar la ropa o joyas que estén cerca de la quemadura, si no están pegadas.", "c"));

            preguntasCurso3.add(new Pregunta(curso3, "¿Cuál es la postura de seguridad o \"posición lateral de seguridad\" (PLS)?", 
                "Una postura que se usa para hacer ejercicios de relajación.", 
                "Una posición para colocar a una persona inconsciente que respira, para mantener la vía aérea abierta y prevenir atragantamientos.", 
                "La postura que debe adoptar el socorrista para levantar peso.", 
                "Una postura para personas con problemas de espalda.", "b"));

            preguntasCurso3.add(new Pregunta(curso3, "¿Cuál es la secuencia correcta de pasos en la RCP para adultos según la formación estándar?", 
                "30 compresiones torácicas y 2 ventilaciones de rescate.", 
                "5 compresiones y 1 ventilación.", 
                "20 compresiones y 5 ventilaciones.", 
                "Solo compresiones, sin ventilaciones.", "a"));

            preguntasCurso3.add(new Pregunta(curso3, "Antes de actuar, la primera pregunta que debe hacerse un socorrista para no poner en riesgo su propia vida es:", 
                "¿Qué le pasará a esta persona?", 
                "¿Hay algún peligro para mí o para la víctima? (electricidad, fuego, tráfico, etc.)", 
                "¿Tengo seguro médico?", 
                "¿Quién es esta persona?", "b"));

            preguntaRepository.saveAll(preguntasCurso3);

            System.out.println("🌱 ¡Cursos y preguntas de capacitación cargados con éxito!");
        }

        // Mostrar resumen de usuarios
        System.out.println("\n📊 RESÚMEN DE USUARIOS Y CAPACITACIÓN EN BD:");
        System.out.println("   Administradores: " + administradorRepository.count());
        System.out.println("   Cursos cargados: " + cursoRepository.count());
        System.out.println("   Preguntas cargadas: " + preguntaRepository.count());
    }
}