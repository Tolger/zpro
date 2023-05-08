package de.zpro.backend.exceptions

sealed abstract class CustomException(message: String, cause: Throwable) extends Exception(message, cause)

sealed abstract class UnexpectedException(message: String = "", cause: Throwable = null) extends CustomException(message, cause)

case class UnknownRelationException(message: String) extends UnexpectedException(message)

case class UnknownChildNameException(message: String) extends UnexpectedException(message)

case class UnknownPropertyTypeException(message: String) extends UnexpectedException(message)

case class ParsingException(message: String, cause: Throwable) extends UnexpectedException(cause = cause)

sealed abstract class ExpectedException(message: String = "", cause: Throwable = null) extends CustomException(message, cause)
