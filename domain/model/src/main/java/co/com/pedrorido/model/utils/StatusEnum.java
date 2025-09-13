package co.com.pedrorido.model.utils;

public enum StatusEnum {
    PENDING(1L, "Pendiente de revisión"),
    APPROVED( 2L, "Aprobado"),
    REJECTED( 3L, "Rechazado");

    private Long id;
    private String description;

    StatusEnum(Long id, String description) {
        this.id = id;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static StatusEnum fromId(Long id) {
        for (StatusEnum status : StatusEnum.values()) {
            if (status.getId().equals(id)) {
                return status;
            }
        }
        return null;
    }
}
