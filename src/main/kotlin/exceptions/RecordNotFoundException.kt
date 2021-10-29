package exceptions

class RecordNotFoundException(key: String) : RuntimeException("Не найде запрос с ключом $key") {

}
