package collins.controllers.validators

import play.api.data.Forms.optional
import play.api.data.Forms.text

import collins.validation.StringUtil

trait ParamValidation {
  protected def validatedText(minLen: Int, maxLen: Int = Int.MaxValue) = text(minLen,maxLen).verifying { txt =>
    StringUtil.trim(txt) match {
      case None => false
      case Some(v) => v.length >= minLen && v.length <= maxLen
    }
  }
  protected def validatedOptionalText(minLen: Int, maxLen: Int = Int.MaxValue) = optional(
    validatedText(minLen, maxLen)
  )
}
