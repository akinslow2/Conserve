package CompanyCamAPI.Enums

enum class Permissions(val value: Int) {
    ModifyUsers(1),
    DeleteUsers(2),
    ModifyGroups(3),
    DeleteGroups(4),
    ManageBilling(5),
    AccessReports(6),
    DeletePhotos(7),
    MovePhotos(8),
    SharePhotos(9),
    ViewUserPhotos(10),
    ViewGroups(11),
    AccessMobile(12),
    ModifyProject(13),
    DeleteProject(14),
    BrowseUserPhotos(15),
    AccessProjectDocuments(16)
}