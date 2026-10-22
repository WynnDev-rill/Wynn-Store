package id.wynn.screenguard.privilege;
interface IGuardService {
    String capabilities() = 0;
    String network(String packageName, int uid, boolean blocked) = 1;
    String freeze(String packageName, int userId, int mode, boolean enabled) = 2;
    void destroy() = 16777114;
}
