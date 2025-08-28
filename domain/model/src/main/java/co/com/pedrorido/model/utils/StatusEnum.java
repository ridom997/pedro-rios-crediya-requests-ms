package co.com.pedrorido.model.utils;

public enum StatusEnum {
    PENDING("1", "Pendiente de revisión"),
    APPROVED( "2", "Aprobado"),
    REJECTED( "3", "Rechazado");

    private String id;
    private String description;

    StatusEnum(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
