package opgg.ghrami.Services;

import java.util.List;

public interface InterfaceCRUD<T>{

    void ajouter(T t);
    void modifier(T t);
    void supprimer(String id);
    List<T> afficher();



}
