package es.academia.campushub.config;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import es.academia.campushub.document.SeguimientoDocument;
import org.bson.Document;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Prepara la colección de seguimientos al arrancar: validación mínima ($jsonSchema) e índices.
 *
 * <p>La validación solo exige los campos comunes (actividadId, tipo, datos, creadoEn) y sus tipos.
 * El contenido de "datos" es libre a propósito, para no perder la flexibilidad que justifica usar
 * MongoDB. Se aplica también si la colección ya existía (collMod).
 */
@Component
public class MongoSchemaInitializer implements InitializingBean {
  private static final String COLECCION = SeguimientoDocument.COLECCION;

  private static final String ESQUEMA =
      """
      {
        "bsonType": "object",
        "required": ["actividadId", "tipo", "datos", "creadoEn"],
        "properties": {
          "actividadId": { "bsonType": ["long", "int"], "minimum": 1 },
          "tipo": { "bsonType": "string", "minLength": 1, "maxLength": 50 },
          "datos": { "bsonType": "object" },
          "creadoEn": { "bsonType": "date" },
          "versionEsquema": { "bsonType": ["int", "long"], "minimum": 1 }
        }
      }
      """;

  private final MongoTemplate mongoTemplate;

  public MongoSchemaInitializer(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public void afterPropertiesSet() {
    Document validador = new Document("$jsonSchema", Document.parse(ESQUEMA));
    MongoDatabase db = mongoTemplate.getDb();

    if (!mongoTemplate.collectionExists(COLECCION)) {
      db.createCollection(
          COLECCION,
          new CreateCollectionOptions()
              .validationOptions(
                  new ValidationOptions()
                      .validator(validador)
                      .validationLevel(ValidationLevel.STRICT)
                      .validationAction(ValidationAction.ERROR)));
    } else {
      db.runCommand(
          new Document("collMod", COLECCION)
              .append("validator", validador)
              .append("validationLevel", "strict")
              .append("validationAction", "error"));
    }

    MongoCollection<Document> coleccion = db.getCollection(COLECCION);
    // Consulta más frecuente: seguimientos de una actividad ordenados por fecha (detalle, RF06).
    coleccion.createIndex(
        Indexes.compoundIndex(Indexes.ascending("actividadId"), Indexes.ascending("creadoEn")),
        new IndexOptions().name("ix_seguimiento_actividad_fecha"));
    // Permite filtrar registros por tipo de seguimiento.
    coleccion.createIndex(
        Indexes.ascending("tipo"), new IndexOptions().name("ix_seguimiento_tipo"));
  }
}
